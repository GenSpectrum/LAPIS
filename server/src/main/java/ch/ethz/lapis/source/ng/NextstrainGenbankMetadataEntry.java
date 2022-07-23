package ch.ethz.lapis.source.ng;

import java.io.Serializable;
import java.time.LocalDate;

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
    private String gisaidClade;
    private String originatingLab;
    private String submittingLab;
    private String authors;
    private LocalDate dateSubmitted;
    private String samplingStrategy;

    public String getStrain() {
        return strain;
    }

    public NextstrainGenbankMetadataEntry setStrain(String strain) {
        this.strain = strain;
        return this;
    }

    public String getVirus() {
        return virus;
    }

    public NextstrainGenbankMetadataEntry setVirus(String virus) {
        this.virus = virus;
        return this;
    }

    public String getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public NextstrainGenbankMetadataEntry setGisaidEpiIsl(String gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }

    public String getGenbankAccession() {
        return genbankAccession;
    }

    public NextstrainGenbankMetadataEntry setGenbankAccession(String genbankAccession) {
        this.genbankAccession = genbankAccession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public NextstrainGenbankMetadataEntry setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public NextstrainGenbankMetadataEntry setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public NextstrainGenbankMetadataEntry setYear(Integer year) {
        this.year = year;
        return this;
    }

    public Integer getMonth() {
        return month;
    }

    public NextstrainGenbankMetadataEntry setMonth(Integer month) {
        this.month = month;
        return this;
    }

    public Integer getDay() {
        return day;
    }

    public NextstrainGenbankMetadataEntry setDay(Integer day) {
        this.day = day;
        return this;
    }

    public String getDateOriginal() {
        return dateOriginal;
    }

    public NextstrainGenbankMetadataEntry setDateOriginal(String dateOriginal) {
        this.dateOriginal = dateOriginal;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public NextstrainGenbankMetadataEntry setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public NextstrainGenbankMetadataEntry setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getDivision() {
        return division;
    }

    public NextstrainGenbankMetadataEntry setDivision(String division) {
        this.division = division;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public NextstrainGenbankMetadataEntry setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getRegionExposure() {
        return regionExposure;
    }

    public NextstrainGenbankMetadataEntry setRegionExposure(String regionExposure) {
        this.regionExposure = regionExposure;
        return this;
    }

    public String getCountryExposure() {
        return countryExposure;
    }

    public NextstrainGenbankMetadataEntry setCountryExposure(String countryExposure) {
        this.countryExposure = countryExposure;
        return this;
    }

    public String getDivisionExposure() {
        return divisionExposure;
    }

    public NextstrainGenbankMetadataEntry setDivisionExposure(String divisionExposure) {
        this.divisionExposure = divisionExposure;
        return this;
    }

    public String getHost() {
        return host;
    }

    public NextstrainGenbankMetadataEntry setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getAge() {
        return age;
    }

    public NextstrainGenbankMetadataEntry setAge(Integer age) {
        this.age = age;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public NextstrainGenbankMetadataEntry setSex(String sex) {
        this.sex = sex;
        return this;
    }

    public String getNextstrainClade() {
        return nextstrainClade;
    }

    public NextstrainGenbankMetadataEntry setNextstrainClade(String nextstrainClade) {
        this.nextstrainClade = nextstrainClade;
        return this;
    }

    public String getPangoLineage() {
        return pangoLineage;
    }

    public NextstrainGenbankMetadataEntry setPangoLineage(String pangoLineage) {
        this.pangoLineage = pangoLineage;
        return this;
    }

    public String getGisaidClade() {
        return gisaidClade;
    }

    public NextstrainGenbankMetadataEntry setGisaidClade(String gisaidClade) {
        this.gisaidClade = gisaidClade;
        return this;
    }

    public String getOriginatingLab() {
        return originatingLab;
    }

    public NextstrainGenbankMetadataEntry setOriginatingLab(String originatingLab) {
        this.originatingLab = originatingLab;
        return this;
    }

    public String getSubmittingLab() {
        return submittingLab;
    }

    public NextstrainGenbankMetadataEntry setSubmittingLab(String submittingLab) {
        this.submittingLab = submittingLab;
        return this;
    }

    public String getAuthors() {
        return authors;
    }

    public NextstrainGenbankMetadataEntry setAuthors(String authors) {
        this.authors = authors;
        return this;
    }

    public LocalDate getDateSubmitted() {
        return dateSubmitted;
    }

    public NextstrainGenbankMetadataEntry setDateSubmitted(LocalDate dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
        return this;
    }

    public String getSamplingStrategy() {
        return samplingStrategy;
    }

    public NextstrainGenbankMetadataEntry setSamplingStrategy(String samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
        return this;
    }
}
