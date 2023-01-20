package ch.ethz.lapis.api.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class StatisticsLogObjectMapper extends ObjectMapper {
    public StatisticsLogObjectMapper(){
        super();
        registerModule(new JavaTimeModule());
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
