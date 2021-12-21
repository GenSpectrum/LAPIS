package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.CacheService;
import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.*;
import ch.ethz.lapis.api.entity.res.*;
import ch.ethz.lapis.api.exception.ForbiddenException;
import ch.ethz.lapis.api.exception.GisaidLimitationException;
import ch.ethz.lapis.api.exception.OutdatedDataVersionException;
import ch.ethz.lapis.api.exception.RedundantVariantDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


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


    @GetMapping(
        value = "/aggregated",
        produces = "application/json"
    )
    public ResponseEntity<String> getAggregated(SampleAggregatedRequest request, GeneralConfig generalConfig) {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        ApiCacheKey cacheKey = new ApiCacheKey(CacheService.SupportedEndpoints.SAMPLE_AGGREGATED, request);
        String body = useCacheOrCompute(cacheKey, () -> {
            try {
                List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request);
                V1Response<SampleAggregatedResponse> response = new V1Response<>(new SampleAggregatedResponse(
                    request.getFields(),
                    aggregatedSamples
                ), dataVersionService.getVersion(), openness);
                return objectMapper.writeValueAsString(response);
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDownloadFileName("aggregated.json")
            .setBody(body)
            .build();
    }


    @GetMapping("/details")
    public ResponseEntity<V1Response<SampleDetailResponse>> getDetails(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
        if (openness == OpennessLevel.GISAID) {
            throw new GisaidLimitationException();
        }
        List<SampleDetail> samples = sampleService.getDetailedSamples(request, limitAndOrder);
        var body = new V1Response<>(new SampleDetailResponse(samples), dataVersionService.getVersion(), openness);
        return new SampleResponseBuilder<V1Response<SampleDetailResponse>>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDownloadFileName("details.json")
            .setBody(body)
            .build();
    }


    @GetMapping("/contributors")
    public ResponseEntity<V1Response<ContributorResponse>> getContributors(
        SampleDetailRequest request,
        GeneralConfig generalConfig,
        OrderAndLimitConfig limitAndOrder
    ) throws SQLException {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
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
        var body = new V1Response<>(new ContributorResponse(contributors), dataVersionService.getVersion(), openness);
        return new SampleResponseBuilder<V1Response<ContributorResponse>>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDownloadFileName("contributors.json")
            .setBody(body)
            .build();
    }


    @GetMapping(
        value = "/strain-names",
        produces = "text/plain"
    )
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
            .setDownloadFileName("strain_names.txt")
            .setBody(body)
            .build();
    }


    @GetMapping(
        value = "/gisaid-epi-isl",
        produces = "text/plain"
    )
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
            .setDownloadFileName("gisaid_epi_isl.txt")
            .setBody(body)
            .build();
    }


    @GetMapping(
        value = "/aa-mutations",
        produces = "application/json"
    )
    public ResponseEntity<String> getAAMutations(MutationRequest request, GeneralConfig generalConfig) {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
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
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDownloadFileName("aa_mutations.json")
            .setBody(body)
            .build();
    }


    @GetMapping(
        value = "/nuc-mutations",
        produces = "application/json"
    )
    public ResponseEntity<String> getNucMutations(MutationRequest request, GeneralConfig generalConfig) {
        checkDataVersion(generalConfig.getDataVersion());
        checkVariantFilter(request);
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
        return new SampleResponseBuilder<String>()
            .setAllowCaching(generalConfig.getDataVersion() != null)
            .setETag(generateETag(cacheKey))
            .setDataVersion(dataVersionService.getVersion())
            .setForDownload(generalConfig.isDownloadAsFile())
            .setDownloadFileName("nuc_mutations.json")
            .setBody(body)
            .build();
    }


    @GetMapping(
        value = "/fasta",
        produces = "text/x-fasta"
    )
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
            .setDownloadFileName("sequences.fasta")
            .setBody(responseBody)
            .build();
    }


    @GetMapping(
        value = "/fasta-aligned",
        produces = "text/x-fasta"
    )
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
            .setDownloadFileName("aligned_sequences.fasta")
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

}
