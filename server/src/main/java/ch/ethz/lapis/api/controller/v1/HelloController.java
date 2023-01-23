package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.SampleService;
import ch.ethz.lapis.api.entity.res.NextcladeDatasetInfoResponse;
import ch.ethz.lapis.api.entity.res.SimpleMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class HelloController {

    private final SampleService sampleService;

    public HelloController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("")
    public SimpleMessage getDataStatus() {
        String s = "This is LAPIS. Please see https://cov-spectrum.ethz.ch/public for more information";
        return new SimpleMessage(s);
    }

    @GetMapping("/info/nextclade-dataset")
    public NextcladeDatasetInfoResponse getNextcladeDatasetInfo() {
        return new NextcladeDatasetInfoResponse(
            sampleService.getNextcladeDatasetTag(),
            "sars-cov-2"
        );
    }

}
