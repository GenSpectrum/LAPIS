package ch.ethz.lapis.api.log;

import ch.ethz.lapis.api.entity.req.SampleFilter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Data
public class RequestContext {
    private long unixTimestamp;
    private long responseTimeInMilliSeconds;
    private String endpoint;
    private boolean returnedDataFromCache = false;
    private SampleFilter filter;
}
