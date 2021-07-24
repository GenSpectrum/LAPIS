package ch.ethz.lapis.api.entity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NucMutationListDeserializer implements Converter<String, List<NucMutation>> {
    @Override
    public List<NucMutation> convert(String value) {
        if (value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(",")).map(NucMutation::parse).collect(Collectors.toList());
    }
}
