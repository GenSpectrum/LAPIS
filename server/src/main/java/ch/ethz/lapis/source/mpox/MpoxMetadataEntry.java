package ch.ethz.lapis.source.mpox;

import java.io.Serializable;
import java.time.LocalDate;

public class MpoxMetadataEntry implements Serializable {

    private String strain;
    private String sraAccession;
    private LocalDate date;
    private String dateOriginal;
    private String region;
    private String country;
    private String host;
    private String clade;

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

    public String getDateOriginal() {
        return dateOriginal;
    }

    public MpoxMetadataEntry setDateOriginal(String dateOriginal) {
        this.dateOriginal = dateOriginal;
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
}
