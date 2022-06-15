package ch.ethz.lapis.api.entity.res;

public class SampleDetail extends SampleMetadata<SampleDetail> {

    private String accession;

    private String sraAccession;

    private String strain;

    public String getAccession() {
        return accession;
    }

    public SampleDetail setAccession(String accession) {
        this.accession = accession;
        return this;
    }

    public String getSraAccession() {
        return sraAccession;
    }

    public SampleDetail setSraAccession(String sraAccession) {
        this.sraAccession = sraAccession;
        return this;
    }

    public String getStrain() {
        return strain;
    }

    public SampleDetail setStrain(String strain) {
        this.strain = strain;
        return this;
    }
}
