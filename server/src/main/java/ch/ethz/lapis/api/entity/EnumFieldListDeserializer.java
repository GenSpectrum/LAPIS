package ch.ethz.lapis.api.entity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EnumFieldListDeserializer<T extends Enum<T>> implements Converter<String, List<T>> {
    // workaround because T.class does not work
    abstract protected Class<T> type();

    @Override
    public List<T> convert(String value) {

        if (value.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(value.split(","))
            .map(v -> T.valueOf(type(), v.toUpperCase()))
            .collect(Collectors.toList());
    }
}
