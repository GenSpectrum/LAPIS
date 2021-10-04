package ch.ethz.lapis.api.entity.res;

public class Contributor {

    private String genbankAccession;
    private String sraAccession;
    private String gisaidEpiIsl;
    private String submittingLab;
    private String originatingLab;
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

    public String getSubmittingLab() {
        return submittingLab;
    }

    public Contributor setSubmittingLab(String submittingLab) {
        this.submittingLab = submittingLab;
        return this;
    }

    public String getOriginatingLab() {
        return originatingLab;
    }

    public Contributor setOriginatingLab(String originatingLab) {
        this.originatingLab = originatingLab;
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
