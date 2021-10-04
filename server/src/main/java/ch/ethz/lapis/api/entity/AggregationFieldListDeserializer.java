package ch.ethz.lapis.api.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AggregationFieldListDeserializer implements Converter<String, List<AggregationField>> {

    @Override
    public List<AggregationField> convert(String value) {
        if (value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
            .map(v -> AggregationField.valueOf(v.toUpperCase()))
            .collect(Collectors.toList());
    }
}
