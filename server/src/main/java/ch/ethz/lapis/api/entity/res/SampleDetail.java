package ch.ethz.lapis.api.entity.res;

public class SampleDetail extends SampleMetadata<SampleDetail> {

    private String genbankAccession;

    private String sraAccession;

    private String gisaidEpiIsl;

    public String getGenbankAccession() {
        return genbankAccession;
    }

    public SampleDetail setGenbankAccession(String genbankAccession) {
        this.genbankAccession = genbankAccession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public SampleDetail setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public String getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public SampleDetail setGisaidEpiIsl(String gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }
}
