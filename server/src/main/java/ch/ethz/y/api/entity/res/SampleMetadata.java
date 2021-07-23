package ch.ethz.y.api.entity.res;

import java.time.LocalDate;

public abstract class SampleMetadata<T extends SampleMetadata<T>> {

    private LocalDate date;
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
    private String host;
    private String samplingStrategy;
    private String pangoLineage;
    private String nextstrainClade;
    private String gisaidCloade;
    private String submittingLab;
    private String originatingLab;

    public LocalDate getDate() {
        return date;
    }

    public T setDate(LocalDate date) {
        this.date = date;
        return (T) this;
    }

    public LocalDate getDateSubmitted() {
        return dateSubmitted;
    }

    public T setDateSubmitted(LocalDate dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
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

    public Integer getAge() {
        return age;
    }

    public T setAge(Integer age) {
        this.age = age;
        return (T) this;
    }

    public String getSex() {
        return sex;
    }

    public T setSex(String sex) {
        this.sex = sex;
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

    public String getGisaidCloade() {
        return gisaidCloade;
    }

    public T setGisaidCloade(String gisaidCloade) {
        this.gisaidCloade = gisaidCloade;
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
}
