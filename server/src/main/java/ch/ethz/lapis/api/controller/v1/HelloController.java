package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.entity.res.Response;
import ch.ethz.lapis.api.entity.res.SimpleMessage;
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
