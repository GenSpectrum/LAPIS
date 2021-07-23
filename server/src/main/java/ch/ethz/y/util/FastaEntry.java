package ch.ethz.y.util;

public class FastaEntry {

    private String sampleName;

    private String seq;

    public FastaEntry(String sampleName, String seq) {
        this.sampleName = sampleName;
        this.seq = seq;
    }

    public String getSampleName() {
        return sampleName;
    }

    public FastaEntry setSampleName(String sampleName) {
        this.sampleName = sampleName;
        return this;
    }

    public String getSeq() {
        return seq;
    }

    public FastaEntry setSeq(String seq) {
        this.seq = seq;
        return this;
    }
}
