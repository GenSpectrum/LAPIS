package ch.ethz.lapis.source.mpox;

import java.io.Serializable;
import java.time.LocalDate;

public class MpoxMetadataEntry implements Serializable {

    private String accession;
    private String accessionRev;
    private String strain;
    private String sraAccession;
    private LocalDate date;
    private Integer year;
    private Integer month;
    private Integer day;
    private String dateOriginal;
    private LocalDate dateSubmitted;
    private String region;
    private String country;
    private String division;
    private String location;
    private String host;
    private String clade;
    private String authors;
    private String institution;

    public String getAccession() {
        return accession;
    }

    public MpoxMetadataEntry setAccession(String accession) {
        this.accession = accession;
        return this;
    }

    public String getAccessionRev() {
        return accessionRev;
    }

    public MpoxMetadataEntry setAccessionRev(String accessionRev) {
        this.accessionRev = accessionRev;
        return this;
    }

    public String getStrain() {
        return strain;
    }

    public MpoxMetadataEntry setStrain(String strain) {
        this.strain = strain;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public MpoxMetadataEntry setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public MpoxMetadataEntry setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public MpoxMetadataEntry setYear(Integer year) {
        this.year = year;
        return this;
    }

    public Integer getMonth() {
        return month;
    }

    public MpoxMetadataEntry setMonth(Integer month) {
        this.month = month;
        return this;
    }

    public Integer getDay() {
        return day;
    }

    public MpoxMetadataEntry setDay(Integer day) {
        this.day = day;
        return this;
    }

    public String getDateOriginal() {
        return dateOriginal;
    }

    public MpoxMetadataEntry setDateOriginal(String dateOriginal) {
        this.dateOriginal = dateOriginal;
        return this;
    }

    public LocalDate getDateSubmitted() {
        return dateSubmitted;
    }

    public MpoxMetadataEntry setDateSubmitted(LocalDate dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public MpoxMetadataEntry setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public MpoxMetadataEntry setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getDivision() {
        return division;
    }

    public MpoxMetadataEntry setDivision(String division) {
        this.division = division;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public MpoxMetadataEntry setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getHost() {
        return host;
    }

    public MpoxMetadataEntry setHost(String host) {
        this.host = host;
        return this;
    }

    public String getClade() {
        return clade;
    }

    public MpoxMetadataEntry setClade(String clade) {
        this.clade = clade;
        return this;
    }

    public String getAuthors() {
        return authors;
    }

    public MpoxMetadataEntry setAuthors(String authors) {
        this.authors = authors;
        return this;
    }

    public String getInstitution() {
        return institution;
    }

    public MpoxMetadataEntry setInstitution(String institution) {
        this.institution = institution;
        return this;
    }
}
