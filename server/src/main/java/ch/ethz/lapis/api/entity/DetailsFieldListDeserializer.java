package ch.ethz.lapis.api.entity;

import ch.ethz.lapis.api.entity.res.SampleDetail;
import org.springframework.stereotype.Component;

@Component
public class DetailsFieldListDeserializer extends EnumFieldListDeserializer<SampleDetail.Fields> {

    @Override
    protected Class<SampleDetail.Fields> type() {
        return SampleDetail.Fields.class;
    }
}
