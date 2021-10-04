package ch.ethz.lapis.api.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AAMutationListDeserializer implements Converter<String, List<AAMutation>> {

    @Override
    public List<AAMutation> convert(String value) {
        if (value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(",")).map(AAMutation::parse).collect(Collectors.toList());
    }
}
