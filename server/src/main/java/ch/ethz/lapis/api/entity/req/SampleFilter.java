package ch.ethz.lapis.api.entity.req;

import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucMutation;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class SampleFilter<T extends SampleFilter<T>> {

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
    private String host;
    private String samplingStrategy;
    private String pangoLineage;
    private String nextstrainClade;
    private String gisaidClade;
    private String submittingLab;
    private String originatingLab;

    private List<NucMutation> nucMutations = new ArrayList<>();
    private List<AAMutation> aaMutations = new ArrayList<>();

    private String variantQuery;

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

    public String getHost() {
        return host;
    }

    public T setHost(String host) {
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

    public List<AAMutation> getAaMutations() {
        return aaMutations;
    }

    public T setAaMutations(List<AAMutation> aaMutations) {
        this.aaMutations = aaMutations;
        return (T) this;
    }

    public String getVariantQuery() {
        return variantQuery;
    }

    public T setVariantQuery(String variantQuery) {
        this.variantQuery = variantQuery;
        return (T) this;
    }
}
