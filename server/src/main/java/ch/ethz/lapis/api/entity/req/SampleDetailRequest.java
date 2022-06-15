package ch.ethz.lapis.api.entity.req;

import java.util.List;

public class SampleDetailRequest extends SampleFilter<SampleDetailRequest> {

    private List<String> accession;

    private List<String> sraAccession;

    private List<String> strain;

    public List<String> getAccession() {
        return accession;
    }

    public SampleDetailRequest setAccession(List<String> accession) {
        this.accession = accession;
        return this;
    }

    public List<String> getSraAccession() {
        return sraAccession;
    }

    public SampleDetailRequest setSraAccession(List<String> sraAccession) {
        this.sraAccession = sraAccession;
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
