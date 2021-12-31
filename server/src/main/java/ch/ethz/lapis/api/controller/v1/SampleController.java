package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.CacheService;
import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.AggregationField;
import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.*;
import ch.ethz.lapis.api.entity.res.*;
import ch.ethz.lapis.api.exception.*;
import ch.ethz.lapis.util.StopWatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/sample")
public class SampleController {

    private final Optional<CacheService> cacheServiceOpt;
    private final SampleService sampleService;
    private final DataVersionService dataVersionService;
    private final ObjectMapper objectMapper;
    private final OpennessLevel openness = LapisMain.globalConfig.getApiOpennessLevel();


    public SampleController(
        Optional<CacheService> cacheServiceOpt,
        SampleService sampleService,
        DataVersionService dataVersionService,
        ObjectMapper objectMapper
    ) {
        this.cacheServiceOpt = cacheServiceOpt;
        this.sampleService = sampleService;
        this.dataVersionService = dataVersionService;
        this.objectMapper = objectMapper;
    }


    @GetMapping("/info")
    public Information getInfo() {
        return new V1Response<>(null, dataVersionService.getVersion(), openness).getInfo();
    }


    @GetMapping("/aggregated")
    public ResponseEntity<String> getAggregated(
        SampleAggregatedRequest request,
        GeneralConfig generalConfig
    ) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Controller checks");
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV));
        stopWatch.round("Cache check");
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_AGGREGATED, request);
        String body = useCacheOrCompute(cacheKey, () -> {
            try {
                List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request, stopWatch);
                stopWatch.round("Result formatting");
                V1Response<SampleAggregatedResponse> response = new V1Response<>(new SampleAggregatedResponse(
                    request.getFields(),
                    aggregatedSamples
                ), dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        if (generalConfig.getDataFormat().equals(DataFormat.CSV)) {
            try {
                V1Response<List<SampleAggregated>> res = objectMapper.readValue(body, new TypeReference<>() {});
                List<String> csvFields = request.getFields().stream()
                    .map(AggregationField::name)
                    .collect(Collectors.toList());
                csvFields.add("count");
                body = new CsvSerializer().serialize(res.getData(), SampleAggregated.class, csvFields);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        stopWatch.stop();
        System.out.println("/aggregated - " + stopWatch.getFormattedResultString());
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


    @GetMapping("/details")
    public ResponseEntity<String> getDetails(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException, JsonProcessingException {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV));
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
            case CSV -> new CsvSerializer().serialize(samples, SampleDetail.class);
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


    @GetMapping("/contributors")
    public ResponseEntity<String> getContributors(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException, IOException {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV));
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
            case CSV -> new CsvSerializer().serialize(contributors, Contributor.class);
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


    @GetMapping("/strain-names")
    public ResponseEntity<String> getStrainNames(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException {
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


    @GetMapping("/gisaid-epi-isl")
    public ResponseEntity<String> getGisaidEpiIsls(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException {
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


    @GetMapping("/aa-mutations")
    public ResponseEntity<String> getAAMutations(MutationRequest request, GeneralConfig generalConfig) {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV));
        if (openness == OpennessLevel.GISAID && (
            request.getGisaidEpiIsl() != null
                || request.getGenbankAccession() != null
                || request.getSraAccession() != null
        )) {
            throw new GisaidLimitationException();
        }
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_AA_MUTATIONS, request);
        String body = useCacheOrCompute(cacheKey, () -> {
            try {
                SampleMutationsResponse mutationsResponse = sampleService.getMutations(request,
                    SequenceType.AMINO_ACID, request.getMinProportion());
                V1Response<SampleMutationsResponse> response = new V1Response<>(mutationsResponse,
                    dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        if (generalConfig.getDataFormat().equals(DataFormat.CSV)) {
            try {
                V1Response<SampleMutationsResponse> res = objectMapper.readValue(body, new TypeReference<>() {});
                body = new CsvSerializer().serialize(res.getData(), SampleMutationsResponse.MutationEntry.class);
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


    @GetMapping("/nuc-mutations")
    public ResponseEntity<String> getNucMutations(MutationRequest request, GeneralConfig generalConfig) {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        checkDataFormat(generalConfig.getDataFormat(), List.of(DataFormat.JSON, DataFormat.CSV));
        if (openness == OpennessLevel.GISAID && (
            request.getGisaidEpiIsl() != null
                || request.getGenbankAccession() != null
                || request.getSraAccession() != null
        )) {
            throw new GisaidLimitationException();
        }
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_NUC_MUTATIONS, request);
        String body = useCacheOrCompute(cacheKey, () -> {
            try {
                SampleMutationsResponse mutationsResponse = sampleService.getMutations(request,
                    SequenceType.NUCLEOTIDE, request.getMinProportion());
                V1Response<SampleMutationsResponse> response = new V1Response<>(mutationsResponse,
                    dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        // If a CSV is requested, deserialize the JSON and serialize as CSV
        if (generalConfig.getDataFormat().equals(DataFormat.CSV)) {
            try {
                V1Response<SampleMutationsResponse> res = objectMapper.readValue(body, new TypeReference<>() {});
                body = new CsvSerializer().serialize(res.getData(), SampleMutationsResponse.MutationEntry.class);
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


    @GetMapping("/fasta")
    public ResponseEntity<StreamingResponseBody> getFasta(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) {
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


    @GetMapping("/fasta-aligned")
    public ResponseEntity<StreamingResponseBody> getAlignedFasta(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) {
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


    private String useCacheOrCompute(ApiCacheKey cacheKey, Supplier<String> compute) {
        if (cacheServiceOpt.isEmpty()) {
            return compute.get();
        }
        CacheService cacheService = cacheServiceOpt.get();
        String cached = cacheService.getCompressedString(cacheKey);
        if (cached != null) {
            System.out.println("Cache hit");
            return cached;
        }
        String response = compute.get();
        System.out.println("Cache miss");
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


    private void checkVariantFilter(SampleFilter<?> request) {
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

}
