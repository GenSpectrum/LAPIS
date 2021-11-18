package ch.ethz.lapis.api.entity.req;

public class SampleDetailRequest extends SampleFilter<SampleDetailRequest> {

    private String genbankAccession;

    private String sraAccession;

    private String gisaidEpiIsl;

    private String strain;

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

    public String getStrain() {
        return strain;
    }

    public SampleDetailRequest setStrain(String strain) {
        this.strain = strain;
        return this;
    }
}
