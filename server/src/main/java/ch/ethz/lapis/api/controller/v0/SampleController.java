package ch.ethz.lapis.api.controller.v0;

import ch.ethz.lapis.api.CacheService;
import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.res.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;


@RestController
@RequestMapping("/v0/sample")
public class SampleController {

    private final CacheService cacheService;
    private final SampleService sampleService;
    private final ObjectMapper objectMapper;


    public SampleController(
            CacheService cacheService,
            SampleService sampleService,
            ObjectMapper objectMapper
    ) {
        this.cacheService = cacheService;
        this.sampleService = sampleService;
        this.objectMapper = objectMapper;
    }


    @GetMapping(
            value = "/aggregated",
            produces = "application/json"
    )
    public String getAggregated(SampleAggregatedRequest request) {
        ApiCacheKey cacheKey = new ApiCacheKey("/v0/sample/aggregated", request);
        return useCacheOrCompute(cacheKey, () -> {
            try {
                List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request);
                V0Response<SampleAggregatedResponse> response = new V0Response<>(new SampleAggregatedResponse(
                        request.getFields(),
                        aggregatedSamples
                ));
                return objectMapper.writeValueAsString(response);
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @GetMapping( "/details")
    public V0Response<SampleMutationsResponse> getDetails(SampleDetailRequest request) throws SQLException {
        SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.AMINO_ACID);
        return new V0Response<>(mutationsResponse);
    }


    @GetMapping(
            value = "/aa-mutations",
            produces = "application/json"
    )
    public String getAAMutations(SampleDetailRequest request) {
        ApiCacheKey cacheKey = new ApiCacheKey("/v0/sample/aa-mutations", request);
        return useCacheOrCompute(cacheKey, () -> {
           try {
               SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.AMINO_ACID);
               V0Response<SampleMutationsResponse> response = new V0Response<>(mutationsResponse);
               return objectMapper.writeValueAsString(response);
           } catch (SQLException | JsonProcessingException e) {
               throw new RuntimeException(e);
           }
        });
    }


    @GetMapping(
            value = "/nuc-mutations",
            produces = "application/json"
    )
    public String getNucMutations(SampleDetailRequest request) {
        ApiCacheKey cacheKey = new ApiCacheKey("/v0/sample/nuc-mutations", request);
        return useCacheOrCompute(cacheKey, () -> {
           try {
               SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.NUCLEOTIDE);
               V0Response<SampleMutationsResponse> response = new V0Response<>(mutationsResponse);
               return objectMapper.writeValueAsString(response);
           } catch (SQLException | JsonProcessingException e) {
               throw new RuntimeException(e);
           }
        });
    }


    @GetMapping(
            value = "/fasta",
            produces = "text/x-fasta"
    )
    public String getFasta(SampleDetailRequest request) throws SQLException {
        return sampleService.getFasta(request, false);
    }


    @GetMapping(
            value = "/fasta-aligned",
            produces = "text/x-fasta"
    )
    public String getAlignedFasta(SampleDetailRequest request) throws SQLException {
        return sampleService.getFasta(request, true);
    }


    private String useCacheOrCompute(ApiCacheKey cacheKey, Supplier<String> compute) {
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

}
