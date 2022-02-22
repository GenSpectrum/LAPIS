package ch.ethz.lapis.api.entity.req;

import java.util.List;

public class SampleDetailRequest extends SampleFilter<SampleDetailRequest> {

    private List<String> genbankAccession;

    private List<String> sraAccession;

    private List<String> gisaidEpiIsl;

    private List<String> strain;

    public List<String> getGenbankAccession() {
        return genbankAccession;
    }

    public SampleDetailRequest setGenbankAccession(List<String> genbankAccession) {
        this.genbankAccession = genbankAccession;
        return this;
    }

    public List<String> getSraAccession() {
        return sraAccession;
    }

    public SampleDetailRequest setSraAccession(List<String> sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public List<String> getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public SampleDetailRequest setGisaidEpiIsl(List<String> gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }

    public List<String> getStrain() {
        return strain;
    }

    public SampleDetailRequest setStrain(List<String> strain) {
        this.strain = strain;
        return this;
    }
}
