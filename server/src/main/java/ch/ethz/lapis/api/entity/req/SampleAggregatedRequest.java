package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.AggregationField;
import java.util.ArrayList;
import java.util.List;

public class SampleAggregatedRequest extends SampleFilter<SampleAggregatedRequest> {

    private List<AggregationField> fields = new ArrayList<>();

    public List<AggregationField> getFields() {
        return fields;
    }

    public SampleAggregatedRequest setFields(List<AggregationField> fields) {
        this.fields = fields;
        return this;
    }
}
