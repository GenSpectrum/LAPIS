package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.CacheService;
import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.*;
import ch.ethz.lapis.api.entity.req.*;
import ch.ethz.lapis.api.entity.res.*;
import ch.ethz.lapis.api.exception.*;
import ch.ethz.lapis.api.log.RequestContext;
import ch.ethz.lapis.api.query.InsertionStore;
import ch.ethz.lapis.util.StopWatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/sample")
@Slf4j
public class SampleController {

    private final Optional<CacheService> cacheServiceOpt;
    private final SampleService sampleService;
    private final DataVersionService dataVersionService;
    private final ObjectMapper objectMapper;
    private final OpennessLevel openness = LapisMain.globalConfig.getApiOpennessLevel();
    private final Map<String, AccessKey.LEVEL> accessKeys;
    private final RequestContext requestContext;


    public SampleController(
        Optional<CacheService> cacheServiceOpt,
        SampleService sampleService,
        DataVersionService dataVersionService,
        ObjectMapper objectMapper,
        RequestContext requestContext
    ) {
        this.cacheServiceOpt = cacheServiceOpt;
        this.sampleService = sampleService;
        this.dataVersionService = dataVersionService;
        this.objectMapper = objectMapper;
        this.requestContext = requestContext;
        if (openness == OpennessLevel.PROTECTED) {
            // TODO The keys are currently only loaded during program start. Later changes will not have an effect
            //  until the next restart.
            accessKeys = new HashMap<>();
            try {
                for (AccessKey accessKey : sampleService.getAccessKeys()) {
                    accessKeys.put(accessKey.getKey(), accessKey.getLevel());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            accessKeys = null;
        }
    }


    @GetMapping("/info")
    public Information getInfo() {
        return new V1Response<>(null, dataVersionService.getVersion(), openness).getInfo();
    }


    @GetMapping("/aggregated")
    public ResponseEntity<String> getAggregated(
        SampleAggregatedRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        boolean endpointServesAggregatedData = request.getStrain() == null
            && request.getGenbankAccession() == null
            && request.getGisaidEpiIsl() == null
            && request.getSraAccession() == null;
        checkAuthorization(accessKey, endpointServesAggregatedData);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Controller checks");
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        stopWatch.round("Cache check");
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_AGGREGATED, request);
        String body = useCacheOrCompute(generalConfig.isNoCache(), cacheKey, () -> {
            try {
                stopWatch.round("Query data");
                Versioned<List<SampleAggregated>> aggregatedSamples = sampleService.getAggregatedSamples(request);
                stopWatch.round("Result formatting");
                V1Response<SampleAggregatedResponse> response = new V1Response<>(new SampleAggregatedResponse(
                    request.getFields(),
                    aggregatedSamples.content()
                ), aggregatedSamples.dataVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        DataFormat dataFormat = generalConfig.getDataFormat();
        if (dataFormat.equals(DataFormat.CSV) || dataFormat.equals(DataFormat.TSV)) {
            try {
                V1Response<List<SampleAggregated>> res = objectMapper.readValue(body, new TypeReference<>() {});
                List<String> csvFields = request.getFields().stream()
                    .map(AggregationField::name)
                    .collect(Collectors.toList());
                csvFields.add("count");
                body = new CsvSerializer(CsvSerializer.getDelimiterFromDataFormat(dataFormat))
                    .serialize(res.getData(), SampleAggregated.class, csvFields);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        stopWatch.stop();
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName("aggregated")
            .setBody(body)
            .build();
    }


    @PostMapping("/aggregated")
    public ResponseEntity<String> getAggregatedPost(
        @RequestBody SampleAggregatedRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getAggregated(request, generalConfig, accessKey);
    }


    @GetMapping("/details")
    public ResponseEntity<String> getDetails(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) throws SQLException, JsonProcessingException {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, false);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        if (openness == OpennessLevel.GISAID) {
            throw new GisaidLimitationException();
        }
        List<SampleDetail> samples = sampleService.getDetailedSamples(request, limitAndOrder);

        String body = switch (generalConfig.getDataFormat()) {
            case JSON -> {
                var response = new V1Response<>(new SampleDetailResponse(samples), dataVersionService.getVersion(),
                    openness);
                yield objectMapper.writeValueAsString(response);
            }
            case CSV -> new CsvSerializer(CsvSerializer.Delimiter.CSV).serialize(samples, SampleDetail.class);
            case TSV -> new CsvSerializer(CsvSerializer.Delimiter.TSV).serialize(samples, SampleDetail.class);
            default -> throw new IllegalStateException("Unexpected value: " + generalConfig.getDataFormat());
        };
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName("details")
            .setBody(body)
            .build();
    }


    @PostMapping("/details")
    public ResponseEntity<String> getDetailsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) throws SQLException, JsonProcessingException {
        return getDetails(request, generalConfig, limitAndOrder, accessKey);
    }

    @GetMapping("/contributors")
    public ResponseEntity<String> getContributors(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) throws SQLException, IOException {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        if (request.getAgeFrom() != null
            || request.getAgeTo() != null
            || request.getSex() != null
            || request.getHospitalized() != null
            || request.getDied() != null
            || request.getFullyVaccinated() != null
        ) {
            throw new ForbiddenException();
        }
        List<Contributor> contributors = sampleService.getContributors(request, limitAndOrder);

        String body = switch (generalConfig.getDataFormat()) {
            case JSON -> {
                var response = new V1Response<>(new ContributorResponse(contributors), dataVersionService.getVersion(),
                    openness);
                yield objectMapper.writeValueAsString(response);
            }
            case CSV -> new CsvSerializer(CsvSerializer.Delimiter.CSV).serialize(contributors, Contributor.class);
            case TSV -> new CsvSerializer(CsvSerializer.Delimiter.TSV).serialize(contributors, Contributor.class);
            default -> throw new IllegalStateException("Unexpected value: " + generalConfig.getDataFormat());
        };
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName("contributors")
            .setBody(body)
            .build();
    }


    @PostMapping("/contributors")
    public ResponseEntity<String> getContributorsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) throws SQLException, IOException {
        return getContributors(request, generalConfig, limitAndOrder, accessKey);
    }


    @GetMapping("/strain-names")
    public ResponseEntity<String> getStrainNames(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    )  {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID && (
            request.getAgeFrom() != null
            || request.getAgeTo() != null
            || request.getSex() != null
            || request.getHospitalized() != null
            || request.getDied() != null
            || request.getFullyVaccinated() != null
        )) {
            throw new ForbiddenException();
        }
        List<String> strainNames = sampleService.getStrainNames(request, limitAndOrder);
        String body = String.join("\n", strainNames);
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(DataFormat.TEXT)
            .setDownloadFileName("strain_names")
            .setBody(body)
            .build();
    }


    @PostMapping("/strain-names")
    public ResponseEntity<String> getStrainNamesPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        return getStrainNames(request, generalConfig, limitAndOrder, accessKey);
    }


    @GetMapping("/gisaid-epi-isl")
    public ResponseEntity<String> getGisaidEpiIsls(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID && (
            request.getAgeFrom() != null
                || request.getAgeTo() != null
                || request.getSex() != null
                || request.getHospitalized() != null
                || request.getDied() != null
                || request.getFullyVaccinated() != null
        )) {
            throw new ForbiddenException();
        }
        List<String> gisaidEpiIsls = sampleService.getGisaidEpiIsls(request, limitAndOrder);
        String body = String.join("\n", gisaidEpiIsls);
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(DataFormat.TEXT)
            .setDownloadFileName("gisaid_epi_isl")
            .setBody(body)
            .build();
    }


    @PostMapping("/gisaid-epi-isl")
    public ResponseEntity<String> getGisaidEpiIslsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        return getGisaidEpiIsls(request, generalConfig, limitAndOrder, accessKey);
    }


    @GetMapping("/genbank-accession")
    public ResponseEntity<String> getGenbankAccessions(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID && (
            request.getAgeFrom() != null
                || request.getAgeTo() != null
                || request.getSex() != null
                || request.getHospitalized() != null
                || request.getDied() != null
                || request.getFullyVaccinated() != null
        )) {
            throw new ForbiddenException();
        }
        List<String> genbankAccessions = sampleService.getGenbankAccessions(request, limitAndOrder);
        String body = String.join("\n", genbankAccessions);
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(DataFormat.TEXT)
            .setDownloadFileName("accessions")
            .setBody(body)
            .build();
    }


    @PostMapping("/genbank-accession")
    public ResponseEntity<String> getGenbankAccessionsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        return getGenbankAccessions(request, generalConfig, limitAndOrder, accessKey);
    }


    @GetMapping("/aa-mutations")
    public ResponseEntity<String> getAAMutations(
        MutationRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        if (openness == OpennessLevel.GISAID && (
            request.getGisaidEpiIsl() != null
                || request.getGenbankAccession() != null
                || request.getSraAccession() != null
        )) {
            throw new GisaidLimitationException();
        }
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_AA_MUTATIONS, request);
        String body = useCacheOrCompute(generalConfig.isNoCache(), cacheKey, () -> {
            try {
                SampleMutationsResponse mutationsResponse = sampleService.getMutations(request,
                    SequenceType.AMINO_ACID, request.getMinProportion());
                V1Response<SampleMutationsResponse> response = new V1Response<>(mutationsResponse,
                    dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        DataFormat dataFormat = generalConfig.getDataFormat();
        if (dataFormat.equals(DataFormat.CSV) || dataFormat.equals(DataFormat.TSV)) {
            try {
                V1Response<SampleMutationsResponse> res = objectMapper.readValue(body, new TypeReference<>() {});
                body = new CsvSerializer(CsvSerializer.getDelimiterFromDataFormat(dataFormat))
                    .serialize(res.getData(), SampleMutationsResponse.MutationEntry.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName("aa_mutations")
            .setBody(body)
            .build();
    }


    @PostMapping("/aa-mutations")
    public ResponseEntity<String> getAAMutationsPost(
        @RequestBody MutationRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getAAMutations(request, generalConfig, accessKey);
    }


    @GetMapping("/nuc-mutations")
    public ResponseEntity<String> getNucMutations(
        MutationRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        if (openness == OpennessLevel.GISAID && (
            request.getGisaidEpiIsl() != null
                || request.getGenbankAccession() != null
                || request.getSraAccession() != null
        )) {
            throw new GisaidLimitationException();
        }
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_NUC_MUTATIONS, request);
        String body = useCacheOrCompute(generalConfig.isNoCache(), cacheKey, () -> {
            try {
                SampleMutationsResponse mutationsResponse = sampleService.getMutations(request,
                    SequenceType.NUCLEOTIDE, request.getMinProportion());
                V1Response<SampleMutationsResponse> response = new V1Response<>(mutationsResponse,
                    dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        DataFormat dataFormat = generalConfig.getDataFormat();
        if (dataFormat.equals(DataFormat.CSV) || dataFormat.equals(DataFormat.TSV)) {
            try {
                V1Response<SampleMutationsResponse> res = objectMapper.readValue(body, new TypeReference<>() {});
                body = new CsvSerializer(CsvSerializer.getDelimiterFromDataFormat(dataFormat))
                    .serialize(res.getData(), SampleMutationsResponse.MutationEntry.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName("nuc_mutations")
            .setBody(body)
            .build();
    }


    @PostMapping("/nuc-mutations")
    public ResponseEntity<String> getNucMutationsPost(
        @RequestBody MutationRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getNucMutations(request, generalConfig, accessKey);
    }


    public ResponseEntity<String> getInsertions(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        String accessKey,
        String endpointName,
        SequenceType sequenceType,
        String fileName
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, true);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV, DataFormat.TSV));
        if (openness == OpennessLevel.GISAID && (
            request.getGisaidEpiIsl() != null
                || request.getGenbankAccession() != null
                || request.getSraAccession() != null
        )) {
            throw new GisaidLimitationException();
        }
        ApiCacheKey cacheKey = new ApiCacheKey(endpointName, request);
        String body = useCacheOrCompute(generalConfig.isNoCache(), cacheKey, () -> {
            try {
                List<InsertionStore.InsertionCount> insertions = sampleService.getInsertions(request, sequenceType);
                V1Response<List<InsertionStore.InsertionCount>> response = new V1Response<>(insertions,
                    dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        DataFormat dataFormat = generalConfig.getDataFormat();
        if (dataFormat.equals(DataFormat.CSV) || dataFormat.equals(DataFormat.TSV)) {
            try {
                V1Response<List<InsertionStore.InsertionCount>> res = objectMapper.readValue(body, new TypeReference<>() {});
                body = new CsvSerializer(CsvSerializer.getDelimiterFromDataFormat(dataFormat))
                    .serialize(res.getData(), InsertionStore.InsertionCount.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(generalConfig.getDataFormat())
            .setDownloadFileName(fileName)
            .setBody(body)
            .build();
    }


    @GetMapping("/aa-insertions")
    public ResponseEntity<String> getAAInsertions(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getInsertions(
            request,
            generalConfig,
            accessKey,
            CacheService.SupportedEndpoints.SAMPLE_AA_INSERTIONS,
            SequenceType.AMINO_ACID,
            "aa-insertions"
        );
    }


    @PostMapping("/aa-insertions")
    public ResponseEntity<String> getAAInsertionsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getAAInsertions(request, generalConfig, accessKey);
    }


    @GetMapping("/nuc-insertions")
    public ResponseEntity<String> getNucInsertions(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
       return getInsertions(
           request,
           generalConfig,
           accessKey,
           CacheService.SupportedEndpoints.SAMPLE_NUC_INSERTIONS,
           SequenceType.NUCLEOTIDE,
           "nuc-insertions"
       );
    }


    @PostMapping("/nuc-insertions")
    public ResponseEntity<String> getNucInsertionsPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        String accessKey
    ) {
        return getNucInsertions(request, generalConfig, accessKey);
    }


    @GetMapping("/fasta")
    public ResponseEntity<StreamingResponseBody> getFasta(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, false);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID) {
            throw new GisaidLimitationException();
        }
        StreamingResponseBody responseBody = response ->
            sampleService.getFasta(request, false, limitAndOrder, response);
        return new SampleResponseBuilder<StreamingResponseBody>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(DataFormat.FASTA)
            .setDownloadFileName("sequences")
            .setBody(responseBody)
            .build();
    }


    @PostMapping("/fasta")
    public ResponseEntity<StreamingResponseBody> getFastaPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        return getFasta(request, generalConfig, limitAndOrder, accessKey);
    }


    @GetMapping("/fasta-aligned")
    public ResponseEntity<StreamingResponseBody> getAlignedFasta(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        requestContext.setVariantFilter(request);

        checkAuthorization(accessKey, false);
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID) {
            throw new GisaidLimitationException();
        }
        StreamingResponseBody responseBody = response ->
            sampleService.getFasta(request, true, limitAndOrder, response);
        return new SampleResponseBuilder<StreamingResponseBody>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDataFormat(DataFormat.FASTA)
            .setDownloadFileName("aligned_sequences")
            .setBody(responseBody)
            .build();
    }


    @PostMapping("/fasta-aligned")
    public ResponseEntity<StreamingResponseBody> getAlignedFastaPost(
        @RequestBody SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder,
        String accessKey
    ) {
        return getAlignedFasta(request, generalConfig, limitAndOrder, accessKey);
    }


    private String useCacheOrCompute(boolean noCache, ApiCacheKey cacheKey, Supplier<String> compute) {
        if (noCache || cacheServiceOpt.isEmpty()) {
            if (noCache) {
                log.info(LocalDateTime.now() + " Ignoring the cache");
            }
            return compute.get();
        }
        CacheService cacheService = cacheServiceOpt.get();
        String cached = cacheService.getCompressedString(cacheKey);
        if (cached != null) {
            log.info("Returning cached result");
            requestContext.setReturnedDataFromCache(true);
            return cached;
        }
        String response = compute.get();
        cacheService.setCompressedString(cacheKey, response);
        return response;
    }


    private String generateETag(ApiCacheKey cacheKey) {
        try {
            String cacheKeyString = objectMapper.writeValueAsString(cacheKey);
            String cacheKeyHash = DigestUtils.md5DigestAsHex(cacheKeyString.getBytes(StandardCharsets.UTF_8));
            return cacheKeyHash + "-" + dataVersionService.getVersion();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private void checkVariantFilter(SampleFilter request) {
        if (request.getVariantQuery() != null &&
            (request.getPangoLineage() != null || request.getNextstrainClade() != null
                || request.getGisaidClade() != null
                || (request.getAaMutations() != null && !request.getAaMutations().isEmpty())
                || (request.getNucMutations() != null && !request.getNucMutations().isEmpty()))) {
            throw new RedundantVariantDefinition();
        }
    }


    /**
     * This function throws an OutdatedDataVersionException if the requested dataVersion does not exist anymore.
     */
    private void checkDataVersion(Long dataVersion) {
        if (dataVersion != null) {
            if (dataVersion != dataVersionService.getVersion()) {
                throw new OutdatedDataVersionException(dataVersion, dataVersionService.getVersion());
            }
        }
    }

    private void checkDataFormat(DataFormat dataFormat, List<DataFormat> supportedFormats) {
        for (DataFormat supportedFormat : supportedFormats) {
            if (supportedFormat.equals(dataFormat)) {
                return;
            }
        }
        throw new UnsupportedDataFormatException(dataFormat);
    }

    private void checkAuthorization(String accessKey, boolean endpointServesAggregatedData) {
        // This function will only get active if the instance is protected.
        if (openness != OpennessLevel.PROTECTED) {
            return;
        }
        if (endpointServesAggregatedData && accessKeys.containsKey(accessKey)) {
            return;
        }
        if (!accessKeys.containsKey(accessKey) || accessKeys.get(accessKey) != AccessKey.LEVEL.FULL) {
            throw new ForbiddenException();
        }
    }

}
