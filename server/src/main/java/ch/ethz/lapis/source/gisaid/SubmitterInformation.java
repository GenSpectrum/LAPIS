package ch.ethz.lapis.source.gisaid;


public class SubmitterInformation {

    private String submittingLab;
    private String originatingLab;
    private String authors;

    public String getSubmittingLab() {
        return submittingLab;
    }

    public SubmitterInformation setSubmittingLab(String submittingLab) {
        this.submittingLab = submittingLab;
        return this;
    }

    public String getOriginatingLab() {
        return originatingLab;
    }

    public SubmitterInformation setOriginatingLab(String originatingLab) {
        this.originatingLab = originatingLab;
        return this;
    }

    public String getAuthors() {
        return authors;
    }

    public SubmitterInformation setAuthors(String authors) {
        this.authors = authors;
        return this;
    }

    @Override
    public String toString() {
        return "SubmitterInformation{" +
            "submittingLab='" + submittingLab + '\'' +
            ", originatingLab='" + originatingLab + '\'' +
            ", authors='" + authors + '\'' +
            '}';
    }
}
