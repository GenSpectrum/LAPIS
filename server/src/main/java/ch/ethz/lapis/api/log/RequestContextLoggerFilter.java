package ch.ethz.lapis.api.log;

import ch.ethz.lapis.util.TimeFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@AllArgsConstructor
@Slf4j
public class RequestContextLoggerFilter extends OncePerRequestFilter {
    private final RequestContext requestContext;
    private final StatisticsLogObjectMapper objectMapper;
    private final Logger statisticsLogger;
    private final TimeFactory timeFactory;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        LocalDateTime before = timeFactory.now();
        requestContext.setTimestamp(before);
        String requestURI = request.getRequestURI();
        requestContext.setEndpoint(requestURI);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (requestURI.contains("/v1/sample") && !requestURI.contains("info")) {
                requestContext.setResponseTimeInSeconds(Duration.between(before, timeFactory.now()));
                try {
                    statisticsLogger.info(objectMapper.writeValueAsString(requestContext));
                } catch (JsonProcessingException e) {
                    log.error("Could not log statistics message: " + e.getMessage(), e);
                }
            }
        }
    }
}

