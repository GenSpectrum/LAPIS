package ch.ethz.lapis.api.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
public class StatisticsLogObjectMapper {

    private final ObjectMapper mapper;

    public StatisticsLogObjectMapper(Jackson2ObjectMapperBuilder objectMapperBuilder){
        mapper = objectMapperBuilder.build();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String writeValueAsString(RequestContext requestContext) throws JsonProcessingException {
        return mapper.writerFor(RequestContext.class).writeValueAsString(requestContext);
    }
}
