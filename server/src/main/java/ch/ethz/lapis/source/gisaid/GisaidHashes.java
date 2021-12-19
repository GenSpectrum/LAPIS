package ch.ethz.lapis.source.gisaid;


public class GisaidHashes {
    private String metadataHash;
    private String seqOriginalHash;

    public GisaidHashes() {
    }

    public GisaidHashes(String metadataHash, String seqOriginalHash) {
        this.metadataHash = metadataHash;
        this.seqOriginalHash = seqOriginalHash;
    }

    public String getMetadataHash() {
        return metadataHash;
    }

    public GisaidHashes setMetadataHash(String metadataHash) {
        this.metadataHash = metadataHash;
        return this;
    }

    public String getSeqOriginalHash() {
        return seqOriginalHash;
    }

    public GisaidHashes setSeqOriginalHash(String seqOriginalHash) {
        this.seqOriginalHash = seqOriginalHash;
        return this;
    }
}
