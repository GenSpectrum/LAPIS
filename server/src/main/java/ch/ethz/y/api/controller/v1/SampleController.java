package ch.ethz.y.api.controller.v1;

import ch.ethz.y.api.SampleService;
import ch.ethz.y.api.entity.req.SampleAggregatedRequest;
import ch.ethz.y.api.entity.req.SampleDetailRequest;
import ch.ethz.y.api.entity.res.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/v1/sample")
public class SampleController {

    private final SampleService sampleService;

    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("/aggregated")
    public Response<SampleAggregatedResponse> getAggregated(SampleAggregatedRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        List<SampleAggregated> aggregatedSamples = sampleService.getAggregatedSamples(request);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return new Response<>(new SampleAggregatedResponse(
                request.getFields(),
                aggregatedSamples
        ));
    }

    @GetMapping("/details")
    public Response<SampleDetailResponse> getDetails(SampleDetailRequest request) throws SQLException {
        long start = System.currentTimeMillis();
        List<SampleDetail> samples = sampleService.getDetailedSamples(request);
        System.out.println("E2E: " + (System.currentTimeMillis() - start));
        return new Response<>(new SampleDetailResponse(samples));
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
