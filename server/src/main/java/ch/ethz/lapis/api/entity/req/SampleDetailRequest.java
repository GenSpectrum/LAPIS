package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.res.SampleDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class SampleDetailRequest extends SampleFilter {
    private List<SampleDetail.Fields> fields = new ArrayList<>();

    public List<String> getFieldsAsStrings() {
        return this.getFields().stream().map(Enum::toString).collect(Collectors.toList());
    }
}
