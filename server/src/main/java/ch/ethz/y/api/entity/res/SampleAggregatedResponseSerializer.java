package ch.ethz.y.api.entity.res;

import ch.ethz.y.api.entity.AggregationField;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class SampleAggregatedResponseSerializer extends JsonSerializer<SampleAggregatedResponse> {
    @Override
    public void serialize(
            SampleAggregatedResponse resultSet,
            JsonGenerator gen,
            SerializerProvider serializers
    ) throws IOException {
        gen.writeStartArray();
        for (SampleAggregated sample : resultSet.getSamples()) {
            gen.writeStartObject();
            for (AggregationField field : resultSet.getFields()) {
                switch (field) {
                    case DATE -> gen.writeObjectField("date", sample.getDate());
                    case DATESUBMITTED -> gen.writeObjectField("date", sample.getDateSubmitted());
                    case REGION -> gen.writeStringField("region", sample.getRegion());
                    case COUNTRY -> gen.writeStringField("country", sample.getCountry());
                    case DIVISION -> gen.writeStringField("division", sample.getDivision());
                    case LOCATION -> gen.writeStringField("location", sample.getLocation());
                    case REGIONEXPOSURE -> gen.writeStringField("regionExposure", sample.getRegionExposure());
                    case COUNTRYEXPOSURE -> gen.writeStringField("countryExposure", sample.getCountryExposure());
                    case DIVISIONEXPOSURE -> gen.writeStringField("divisionExposure", sample.getDivisionExposure());
                    case AGE -> gen.writeObjectField("age", sample.getAge());
                    case SEX -> gen.writeStringField("sex", sample.getSex());
                    case HOST -> gen.writeStringField("host", sample.getHost());
                    case SAMPLINGSTRATEGY -> gen.writeStringField("samplingStrategy", sample.getSamplingStrategy());
                    case PANGOLINEAGE -> gen.writeStringField("pangoLineage", sample.getPangoLineage());
                    case NEXTSTRAINCLADE -> gen.writeStringField("nextstrainClade", sample.getNextstrainClade());
                    case GISAIDCLADE -> gen.writeStringField("gisaidClade", sample.getGisaidCloade());
                    case SUBMITTINGLAB -> gen.writeStringField("submittingLab", sample.getSubmittingLab());
                    case ORIGINATINGLAB -> gen.writeStringField("originatingLab", sample.getOriginatingLab());
                    default -> throw new RuntimeException("Unexpected field name: " + field);
                }
            }
            gen.writeNumberField("count", sample.getCount());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
