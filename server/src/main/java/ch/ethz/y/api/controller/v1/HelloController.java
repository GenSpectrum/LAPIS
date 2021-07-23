package ch.ethz.y.api.controller.v1;

import ch.ethz.y.api.entity.res.Response;
import ch.ethz.y.api.entity.res.SimpleMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class HelloController {

    @GetMapping("")
    public Response<SimpleMessage> getDataStatus() {
        return new Response<>(new SimpleMessage("Hello, I am Y."));
    }

}
