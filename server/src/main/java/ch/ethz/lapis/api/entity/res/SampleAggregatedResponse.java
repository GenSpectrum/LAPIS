package ch.ethz.lapis.api.entity.res;

import ch.ethz.lapis.api.entity.AggregationField;
import java.util.List;

public record SampleAggregatedResponse(List<AggregationField> fields, List<SampleAggregated> samples) {
}
