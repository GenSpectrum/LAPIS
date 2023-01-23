package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.DataVersionService;
import ch.ethz.lapis.api.log.RequestContext;
import ch.ethz.lapis.api.log.StatisticsLogObjectMapper;
import ch.ethz.lapis.util.TimeFactory;
import org.junit.runner.Request;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import static org.mockito.Mockito.mock;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@TestConfiguration
@Import({RequestContext.class, TimeFactory.class, StatisticsLogObjectMapper.class})
public class TestConfig {

    @Bean
    public DataVersionService dataVersionService() {
        return mock(DataVersionService.class);
    }
}
