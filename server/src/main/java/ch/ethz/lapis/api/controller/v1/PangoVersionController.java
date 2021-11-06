package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.SampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/v1/pango-versions")
public class PangoVersionController {

    private final SampleService sampleService;
    private final DataVersionService dataVersionService;
    private String cachedResult;
    private Long cachedDataVersion;

    public PangoVersionController(
        SampleService sampleService,
        DataVersionService dataVersionService
    ) {
        this.sampleService = sampleService;
        this.dataVersionService = dataVersionService;
    }

    @GetMapping("")
    public String getPangoVersions() throws SQLException {
        // Use cache if the cache is not out-dated
        if (cachedResult != null && cachedDataVersion == dataVersionService.getVersion()) {
            return cachedResult;
        }
        // Fetch data
        cachedResult = sampleService.getPangoVersions();
        cachedDataVersion = dataVersionService.getVersion();
        return cachedResult;
    }

}
