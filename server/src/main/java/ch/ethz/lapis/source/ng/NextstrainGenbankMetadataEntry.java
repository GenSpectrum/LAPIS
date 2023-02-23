package ch.ethz.lapis.source.ng;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class NextstrainGenbankMetadataEntry implements Serializable {

    private String strain;
    private String virus;
    private String gisaidEpiIsl;
    private String genbankAccession;
    private String sraAccession;
    private LocalDate date;
    private Integer year;
    private Integer month;
    private Integer day;
    private String dateOriginal;
    private String region;
    private String country;
    private String division;
    private String location;
    private String regionExposure;
    private String countryExposure;
    private String divisionExposure;
    private String host;
    private Integer age;
    private String sex;
    private String nextstrainClade;
    private String pangoLineage;
    private String nextcladePangoLineage;
    private String gisaidClade;
    private String originatingLab;
    private String submittingLab;
    private String authors;
    private LocalDate dateSubmitted;
    private String samplingStrategy;
    private String database;
}
