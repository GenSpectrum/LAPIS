package ch.ethz.lapis.source.mpox;

import java.io.Serializable;
import java.time.LocalDate;

public class MpoxMetadataEntry implements Serializable {

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
    private String host;
    private String clade;
    private String authors;

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
}
