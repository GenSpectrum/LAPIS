package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.api.entity.res.SimpleMessage;
import ch.ethz.lapis.api.entity.res.V1Response;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class HelloController {

    private final OpennessLevel openness = LapisMain.globalConfig.getApiOpennessLevel();

    @GetMapping("")
    public V1Response<SimpleMessage> getDataStatus() {
        String s = "This is LAPIS. Please see https://cov-spectrum.ethz.ch/public for more information";
        return new V1Response<>(new SimpleMessage(s), Instant.now().getEpochSecond(), openness);
    }

}
