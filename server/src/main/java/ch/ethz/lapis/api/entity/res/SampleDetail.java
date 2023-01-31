package ch.ethz.lapis.api.entity.res;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@JsonFilter(SampleDetail.FILTER_NAME)
@FieldNameConstants(asEnum = true)
@Data
@Accessors(chain = true)
public class SampleDetail {

    public final static String FILTER_NAME = "SampleDetailFilter";

    private String genbankAccession;
    private String sraAccession;
    private String gisaidEpiIsl;
    private String strain;
    private LocalDate date;
    private Integer year;
    private Integer month;
    private LocalDate dateSubmitted;
    private String region;
    private String country;
    private String division;
    private String location;
    private String regionExposure;
    private String countryExposure;
    private String divisionExposure;
    private Integer age;
    private String sex;
    private Boolean hospitalized;
    private Boolean died;
    private Boolean fullyVaccinated;
    private String host;
    private String samplingStrategy;
    private String pangoLineage;
    private String nextcladePangoLineage;
    private String nextstrainClade;
    private String gisaidCloade;
    private String submittingLab;
    private String originatingLab;

}
