package ch.ethz.lapis.api.entity.req;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DataFormatDeserializer implements Converter<String, DataFormat> {

    @Override
    public DataFormat convert(String value) {
        if (value.isBlank()) {
            return DataFormat.JSON;
        }
        return DataFormat.valueOf(value.toUpperCase());
    }
}
