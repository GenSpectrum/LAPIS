package ch.ethz.y.api.entity.req;

public class SampleDetailRequest extends SampleFilter<SampleDetailRequest> {

    private String genbankAccession;

    private String sraAccession;

    private String gisaidEpiIsl;

    public String getGenbankAccession() {
        return genbankAccession;
    }

    public SampleDetailRequest setGenbankAccession(String genbankAccession) {
        this.genbankAccession = genbankAccession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public SampleDetailRequest setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public String getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public SampleDetailRequest setGisaidEpiIsl(String gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }
}
