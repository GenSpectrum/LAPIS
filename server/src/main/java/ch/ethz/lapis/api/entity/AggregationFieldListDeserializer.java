package ch.ethz.lapis.api.entity;

import org.springframework.stereotype.Component;

@Component
public class AggregationFieldListDeserializer extends EnumFieldListDeserializer<AggregationField> {

    @Override
    protected Class<AggregationField> type() {
        return AggregationField.class;
    }
}
