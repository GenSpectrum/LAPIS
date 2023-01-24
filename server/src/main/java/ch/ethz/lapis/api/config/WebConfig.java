package ch.ethz.lapis.api.config;

import ch.ethz.lapis.api.log.RequestContext;
import ch.ethz.lapis.api.log.RequestContextLoggerFilter;
import ch.ethz.lapis.api.log.StatisticsLogObjectMapper;
import ch.ethz.lapis.util.TimeFactory;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
            = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }

    @Bean
    public RequestContextLoggerFilter requestContextLoggerFilter(
        RequestContext requestContext,
        StatisticsLogObjectMapper objectMapper,
        TimeFactory timeFactory
    ) {
        return new RequestContextLoggerFilter(
            requestContext,
            objectMapper,
            LoggerFactory.getLogger("StatisticsLogger"),
            timeFactory
        );
    }
}
