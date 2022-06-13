package ch.ethz.lapis.api.entity.res;

public class Contributor {

    private String genbankAccession;
    private String sraAccession;
    private String gisaidEpiIsl;
    private String strain;
    private String institution;
    private String authors;

    public String getGenbankAccession() {
        return genbankAccession;
    }

    public Contributor setGenbankAccession(String genbankAccession) {
        this.genbankAccession = genbankAccession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public Contributor setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public String getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public Contributor setGisaidEpiIsl(String gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }

    public String getStrain() {
        return strain;
    }

    public Contributor setStrain(String strain) {
        this.strain = strain;
        return this;
    }

    public String getInstitution() {
        return institution;
    }

    public Contributor setInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public String getAuthors() {
        return authors;
    }

    public Contributor setAuthors(String authors) {
        this.authors = authors;
        return this;
    }
}
