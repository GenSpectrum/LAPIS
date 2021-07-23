package ch.ethz.y.api.entity.res;

import ch.ethz.y.api.entity.AggregationField;

import java.util.List;

public class SampleAggregatedResponse {

    private final List<AggregationField> fields;

    private final List<SampleAggregated> samples;

    public SampleAggregatedResponse(List<AggregationField> fields, List<SampleAggregated> samples) {
        this.fields = fields;
        this.samples = samples;
    }

    public List<AggregationField> getFields() {
        return fields;
    }

    public List<SampleAggregated> getSamples() {
        return samples;
    }
}
