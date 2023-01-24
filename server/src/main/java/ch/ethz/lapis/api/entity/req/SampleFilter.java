package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.AAInsertion;
import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucInsertion;
import ch.ethz.lapis.api.entity.NucMutation;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public abstract class SampleFilter {

    private List<String> genbankAccession;

    private List<String> sraAccession;

    private List<String> gisaidEpiIsl;

    private List<String> strain;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateSubmittedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateSubmittedTo;

    private Integer yearFrom;
    private Integer yearTo;
    private String yearMonthFrom;
    private String yearMonthTo;

    private String region;
    private String country;
    private String division;
    private String location;
    private String regionExposure;
    private String countryExposure;
    private String divisionExposure;
    private Integer ageFrom;
    private Integer ageTo;
    private String sex;
    private Boolean hospitalized;
    private Boolean died;
    private Boolean fullyVaccinated;
    private List<String> host;
    private String samplingStrategy;
    private String pangoLineage;
    private String nextcladePangoLineage;
    private String nextstrainClade;
    private String gisaidClade;
    private String submittingLab;
    private String originatingLab;
    private Float nextcladeQcOverallScoreFrom;
    private Float nextcladeQcOverallScoreTo;
    private Float nextcladeQcMissingDataScoreFrom;
    private Float nextcladeQcMissingDataScoreTo;
    private Float nextcladeQcMixedSitesScoreFrom;
    private Float nextcladeQcMixedSitesScoreTo;
    private Float nextcladeQcPrivateMutationsScoreFrom;
    private Float nextcladeQcPrivateMutationsScoreTo;
    private Float nextcladeQcSnpClustersScoreFrom;
    private Float nextcladeQcSnpClustersScoreTo;
    private Float nextcladeQcFrameShiftsScoreFrom;
    private Float nextcladeQcFrameShiftsScoreTo;
    private Float nextcladeQcStopCodonsScoreFrom;
    private Float nextcladeQcStopCodonsScoreTo;
    private Float nextcladeCoverageFrom;
    private Float nextcladeCoverageTo;

    private List<NucMutation> nucMutations = new ArrayList<>();
    private List<NucInsertion> nucInsertions = new ArrayList<>();
    private List<AAMutation> aaMutations = new ArrayList<>();
    private List<AAInsertion> aaInsertions = new ArrayList<>();

    private String variantQuery;
}
