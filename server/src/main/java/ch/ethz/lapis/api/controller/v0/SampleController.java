package ch.ethz.lapis.api.controller.v0;

import ch.ethz.lapis.api.SampleService;
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

@RestController
@RequestMapping("/v0/sample")
public class SampleController {

    private final SampleService sampleService;
    private final ObjectMapper objectMapper;

    public SampleController(SampleService sampleService, ObjectMapper objectMapper) {
        this.sampleService = sampleService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(
            value = "/aggregated",
            produces = "application/json"
    )
    public String getAggregated(SampleAggregatedRequest request) throws SQLException, JsonProcessingException {
        List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request);
        V0Response<SampleAggregatedResponse> response = new V0Response<>(new SampleAggregatedResponse(
                request.getFields(),
                aggregatedSamples
        ));
        return objectMapper.writeValueAsString(response);
    }

    @GetMapping(
            value = "/details",
            produces = "application/json"
    )
    public String getDetails(SampleDetailRequest request) throws SQLException, JsonProcessingException {
        List<SampleDetail> samples = sampleService.getDetailedSamples(request);
        V0Response<SampleDetailResponse> response = new V0Response<>(new SampleDetailResponse(samples));
        return objectMapper.writeValueAsString(response);
    }

    @GetMapping(
            value = "/aa-mutations",
            produces = "application/json"
    )
    public String getAAMutations(SampleDetailRequest request) throws SQLException, JsonProcessingException {
        SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.AMINO_ACID);
        V0Response<SampleMutationsResponse> response = new V0Response<>(mutationsResponse);
        return objectMapper.writeValueAsString(response);
    }

    @GetMapping(
            value = "/nuc-mutations",
            produces = "application/json"
    )
    public String getNucMutations(SampleDetailRequest request) throws SQLException, JsonProcessingException {
        SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.NUCLEOTIDE);
        V0Response<SampleMutationsResponse> response = new V0Response<>(mutationsResponse);
        return objectMapper.writeValueAsString(response);
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

}
