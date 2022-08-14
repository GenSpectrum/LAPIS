package ch.ethz.lapis.api.entity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AAInsertionListDeserializer implements Converter<String, List<AAInsertion>> {

    @Override
    public List<AAInsertion> convert(String value) {
        if (value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(",")).map(AAInsertion::parse).collect(Collectors.toList());
    }
}
