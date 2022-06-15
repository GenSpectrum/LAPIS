package ch.ethz.lapis.api.entity.res;

public class Contributor {

    private String accession;
    private String sraAccession;
    private String strain;
    private String institution;
    private String authors;

    public String getAccession() {
        return accession;
    }

    public Contributor setAccession(String accession) {
        this.accession = accession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public Contributor setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
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
