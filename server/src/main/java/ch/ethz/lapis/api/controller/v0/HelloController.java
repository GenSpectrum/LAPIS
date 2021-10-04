package ch.ethz.lapis.api.controller.v0;

import ch.ethz.lapis.api.entity.res.SimpleMessage;
import ch.ethz.lapis.api.entity.res.V0Response;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v0")
public class HelloController {

    @GetMapping("")
    public V0Response<SimpleMessage> getDataStatus() {
        String s = "This is LAPIS. Please see https://cov-spectrum.ethz.ch/public for more information";
        return new V0Response<>(new SimpleMessage(s), Instant.now().getEpochSecond());
    }

}
