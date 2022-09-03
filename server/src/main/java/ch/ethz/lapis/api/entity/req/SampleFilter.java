package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.AAInsertion;
import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucInsertion;
import ch.ethz.lapis.api.entity.NucMutation;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class SampleFilter<T extends SampleFilter<T>> {

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

    public List<String> getGenbankAccession() {
        return genbankAccession;
    }

    public T setGenbankAccession(List<String> genbankAccession) {
        this.genbankAccession = genbankAccession;
        return (T) this;
    }

    public List<String> getSraAccession() {
        return sraAccession;
    }

    public T setSraAccession(List<String> sraAccession) {
        this.sraAccession = sraAccession;
        return (T) this;
    }

    public List<String> getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public T setGisaidEpiIsl(List<String> gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return (T) this;
    }

    public List<String> getStrain() {
        return strain;
    }

    public T setStrain(List<String> strain) {
        this.strain = strain;
        return (T) this;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public T setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
        return (T) this;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public T setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
        return (T) this;
    }

    public LocalDate getDateSubmittedFrom() {
        return dateSubmittedFrom;
    }

    public T setDateSubmittedFrom(LocalDate dateSubmittedFrom) {
        this.dateSubmittedFrom = dateSubmittedFrom;
        return (T) this;
    }

    public LocalDate getDateSubmittedTo() {
        return dateSubmittedTo;
    }

    public T setDateSubmittedTo(LocalDate dateSubmittedTo) {
        this.dateSubmittedTo = dateSubmittedTo;
        return (T) this;
    }

    public String getRegion() {
        return region;
    }

    public T setRegion(String region) {
        this.region = region;
        return (T) this;
    }

    public String getCountry() {
        return country;
    }

    public T setCountry(String country) {
        this.country = country;
        return (T) this;
    }

    public String getDivision() {
        return division;
    }

    public T setDivision(String division) {
        this.division = division;
        return (T) this;
    }

    public String getLocation() {
        return location;
    }

    public T setLocation(String location) {
        this.location = location;
        return (T) this;
    }

    public String getRegionExposure() {
        return regionExposure;
    }

    public T setRegionExposure(String regionExposure) {
        this.regionExposure = regionExposure;
        return (T) this;
    }

    public String getCountryExposure() {
        return countryExposure;
    }

    public T setCountryExposure(String countryExposure) {
        this.countryExposure = countryExposure;
        return (T) this;
    }

    public String getDivisionExposure() {
        return divisionExposure;
    }

    public T setDivisionExposure(String divisionExposure) {
        this.divisionExposure = divisionExposure;
        return (T) this;
    }

    public Integer getAgeFrom() {
        return ageFrom;
    }

    public T setAgeFrom(Integer ageFrom) {
        this.ageFrom = ageFrom;
        return (T) this;
    }

    public Integer getAgeTo() {
        return ageTo;
    }

    public T setAgeTo(Integer ageTo) {
        this.ageTo = ageTo;
        return (T) this;
    }

    public String getSex() {
        return sex;
    }

    public T setSex(String sex) {
        this.sex = sex;
        return (T) this;
    }

    public Boolean getHospitalized() {
        return hospitalized;
    }

    public T setHospitalized(Boolean hospitalized) {
        this.hospitalized = hospitalized;
        return (T) this;
    }

    public Boolean getDied() {
        return died;
    }

    public T setDied(Boolean died) {
        this.died = died;
        return (T) this;
    }

    public Boolean getFullyVaccinated() {
        return fullyVaccinated;
    }

    public T setFullyVaccinated(Boolean fullyVaccinated) {
        this.fullyVaccinated = fullyVaccinated;
        return (T) this;
    }

    public List<String> getHost() {
        return host;
    }

    public T setHost(List<String> host) {
        this.host = host;
        return (T) this;
    }

    public String getSamplingStrategy() {
        return samplingStrategy;
    }

    public T setSamplingStrategy(String samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
        return (T) this;
    }

    public String getPangoLineage() {
        return pangoLineage;
    }

    public T setPangoLineage(String pangoLineage) {
        this.pangoLineage = pangoLineage;
        return (T) this;
    }

    public String getNextcladePangoLineage() {
        return nextcladePangoLineage;
    }

    public T setNextcladePangoLineage(String nextcladePangoLineage) {
        this.nextcladePangoLineage = nextcladePangoLineage;
        return (T) this;
    }

    public String getNextstrainClade() {
        return nextstrainClade;
    }

    public T setNextstrainClade(String nextstrainClade) {
        this.nextstrainClade = nextstrainClade;
        return (T) this;
    }

    public String getGisaidClade() {
        return gisaidClade;
    }

    public T setGisaidClade(String gisaidClade) {
        this.gisaidClade = gisaidClade;
        return (T) this;
    }

    public String getSubmittingLab() {
        return submittingLab;
    }

    public T setSubmittingLab(String submittingLab) {
        this.submittingLab = submittingLab;
        return (T) this;
    }

    public String getOriginatingLab() {
        return originatingLab;
    }

    public T setOriginatingLab(String originatingLab) {
        this.originatingLab = originatingLab;
        return (T) this;
    }

    public List<NucMutation> getNucMutations() {
        return nucMutations;
    }

    public T setNucMutations(List<NucMutation> nucMutations) {
        this.nucMutations = nucMutations;
        return (T) this;
    }

    public List<NucInsertion> getNucInsertions() {
        return nucInsertions;
    }

    public T setNucInsertions(List<NucInsertion> nucInsertions) {
        this.nucInsertions = nucInsertions;
        return (T) this;
    }

    public List<AAMutation> getAaMutations() {
        return aaMutations;
    }

    public T setAaMutations(List<AAMutation> aaMutations) {
        this.aaMutations = aaMutations;
        return (T) this;
    }

    public List<AAInsertion> getAaInsertions() {
        return aaInsertions;
    }

    public T setAaInsertions(List<AAInsertion> aaInsertions) {
        this.aaInsertions = aaInsertions;
        return (T) this;
    }

    public String getVariantQuery() {
        return variantQuery;
    }

    public T setVariantQuery(String variantQuery) {
        this.variantQuery = variantQuery;
        return (T) this;
    }

    public Float getNextcladeQcOverallScoreFrom() {
        return nextcladeQcOverallScoreFrom;
    }

    public T setNextcladeQcOverallScoreFrom(Float nextcladeQcOverallScoreFrom) {
        this.nextcladeQcOverallScoreFrom = nextcladeQcOverallScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcOverallScoreTo() {
        return nextcladeQcOverallScoreTo;
    }

    public T setNextcladeQcOverallScoreTo(Float nextcladeQcOverallScoreTo) {
        this.nextcladeQcOverallScoreTo = nextcladeQcOverallScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcMissingDataScoreFrom() {
        return nextcladeQcMissingDataScoreFrom;
    }

    public T setNextcladeQcMissingDataScoreFrom(Float nextcladeQcMissingDataScoreFrom) {
        this.nextcladeQcMissingDataScoreFrom = nextcladeQcMissingDataScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcMissingDataScoreTo() {
        return nextcladeQcMissingDataScoreTo;
    }

    public T setNextcladeQcMissingDataScoreTo(Float nextcladeQcMissingDataScoreTo) {
        this.nextcladeQcMissingDataScoreTo = nextcladeQcMissingDataScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcMixedSitesScoreFrom() {
        return nextcladeQcMixedSitesScoreFrom;
    }

    public T setNextcladeQcMixedSitesScoreFrom(Float nextcladeQcMixedSitesScoreFrom) {
        this.nextcladeQcMixedSitesScoreFrom = nextcladeQcMixedSitesScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcMixedSitesScoreTo() {
        return nextcladeQcMixedSitesScoreTo;
    }

    public T setNextcladeQcMixedSitesScoreTo(Float nextcladeQcMixedSitesScoreTo) {
        this.nextcladeQcMixedSitesScoreTo = nextcladeQcMixedSitesScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcPrivateMutationsScoreFrom() {
        return nextcladeQcPrivateMutationsScoreFrom;
    }

    public T setNextcladeQcPrivateMutationsScoreFrom(Float nextcladeQcPrivateMutationsScoreFrom) {
        this.nextcladeQcPrivateMutationsScoreFrom = nextcladeQcPrivateMutationsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcPrivateMutationsScoreTo() {
        return nextcladeQcPrivateMutationsScoreTo;
    }

    public T setNextcladeQcPrivateMutationsScoreTo(Float nextcladeQcPrivateMutationsScoreTo) {
        this.nextcladeQcPrivateMutationsScoreTo = nextcladeQcPrivateMutationsScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcSnpClustersScoreFrom() {
        return nextcladeQcSnpClustersScoreFrom;
    }

    public T setNextcladeQcSnpClustersScoreFrom(Float nextcladeQcSnpClustersScoreFrom) {
        this.nextcladeQcSnpClustersScoreFrom = nextcladeQcSnpClustersScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcSnpClustersScoreTo() {
        return nextcladeQcSnpClustersScoreTo;
    }

    public T setNextcladeQcSnpClustersScoreTo(Float nextcladeQcSnpClustersScoreTo) {
        this.nextcladeQcSnpClustersScoreTo = nextcladeQcSnpClustersScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcFrameShiftsScoreFrom() {
        return nextcladeQcFrameShiftsScoreFrom;
    }

    public T setNextcladeQcFrameShiftsScoreFrom(Float nextcladeQcFrameShiftsScoreFrom) {
        this.nextcladeQcFrameShiftsScoreFrom = nextcladeQcFrameShiftsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcFrameShiftsScoreTo() {
        return nextcladeQcFrameShiftsScoreTo;
    }

    public T setNextcladeQcFrameShiftsScoreTo(Float nextcladeQcFrameShiftsScoreTo) {
        this.nextcladeQcFrameShiftsScoreTo = nextcladeQcFrameShiftsScoreTo;
        return (T) this;
    }

    public Float getNextcladeQcStopCodonsScoreFrom() {
        return nextcladeQcStopCodonsScoreFrom;
    }

    public T setNextcladeQcStopCodonsScoreFrom(Float nextcladeQcStopCodonsScoreFrom) {
        this.nextcladeQcStopCodonsScoreFrom = nextcladeQcStopCodonsScoreFrom;
        return (T) this;
    }

    public Float getNextcladeQcStopCodonsScoreTo() {
        return nextcladeQcStopCodonsScoreTo;
    }

    public T setNextcladeQcStopCodonsScoreTo(Float nextcladeQcStopCodonsScoreTo) {
        this.nextcladeQcStopCodonsScoreTo = nextcladeQcStopCodonsScoreTo;
        return (T) this;
    }

    public Float getNextcladeCoverageFrom() {
        return nextcladeCoverageFrom;
    }

    public T setNextcladeCoverageFrom(Float nextcladeCoverageFrom) {
        this.nextcladeCoverageFrom = nextcladeCoverageFrom;
        return (T) this;
    }

    public Float getNextcladeCoverageTo() {
        return nextcladeCoverageTo;
    }

    public T setNextcladeCoverageTo(Float nextcladeCoverageTo) {
        this.nextcladeCoverageTo = nextcladeCoverageTo;
        return (T) this;
    }
}
