package ch.ethz.lapis.api.controller.v0;

import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.res.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/v0/sample")
public class SampleController {

    private final SampleService sampleService;

    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("/aggregated")
    public V0Response<SampleAggregatedResponse> getAggregated(SampleAggregatedRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return new V0Response<>(new SampleAggregatedResponse(
                request.getFields(),
                aggregatedSamples
        ));
    }

    @GetMapping("/details")
    public V0Response<SampleDetailResponse> getDetails(SampleDetailRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        List<SampleDetail> samples = sampleService.getDetailedSamples(request);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return new V0Response<>(new SampleDetailResponse(samples));
    }

    @GetMapping("/aa-mutations")
    public V0Response<SampleMutationsResponse> getAAMutations(SampleDetailRequest request) throws SQLException {
        SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.AMINO_ACID);
        return new V0Response<>(mutationsResponse);
    }

    @GetMapping("/nuc-mutations")
    public V0Response<SampleMutationsResponse> getNucMutations(SampleDetailRequest request) throws SQLException {
        SampleMutationsResponse mutationsResponse = sampleService.getMutations(request, SequenceType.NUCLEOTIDE);
        return new V0Response<>(mutationsResponse);
    }

    @GetMapping(
            value = "/fasta",
            produces = "text/x-fasta"
    )
    public String getFasta(SampleDetailRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        String fasta = sampleService.getFasta(request, false);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return fasta;
    }

    @GetMapping(
            value = "/fasta-aligned",
            produces = "text/x-fasta"
    )
    public String getAlignedFasta(SampleDetailRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        String fasta = sampleService.getFasta(request, true);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return fasta;
    }

}
