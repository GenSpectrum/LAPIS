package ch.ethz.lapis.api.log;

import ch.ethz.lapis.api.entity.req.SampleFilter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequestScope
@Data
public class RequestContext {
    private LocalDateTime timestamp;
    private Duration responseTimeInSeconds;
    private String endpoint;
    private boolean returnedDataFromCache = false;
    private SampleFilter variantFilter;
}
