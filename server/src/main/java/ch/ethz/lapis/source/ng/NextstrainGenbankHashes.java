package ch.ethz.lapis.source.ng;


public class NextstrainGenbankHashes {

    private String metadataHash;
    private String seqOriginalHash;
    private String seqAlignedHash;

    public NextstrainGenbankHashes() {
    }

    public NextstrainGenbankHashes(String metadataHash, String seqOriginalHash, String seqAlignedHash) {
        this.metadataHash = metadataHash;
        this.seqOriginalHash = seqOriginalHash;
        this.seqAlignedHash = seqAlignedHash;
    }

    public String getMetadataHash() {
        return metadataHash;
    }

    public NextstrainGenbankHashes setMetadataHash(String metadataHash) {
        this.metadataHash = metadataHash;
        return this;
    }

    public String getSeqAlignedHash() {
        return seqAlignedHash;
    }

    public NextstrainGenbankHashes setSeqAlignedHash(String seqAlignedHash) {
        this.seqAlignedHash = seqAlignedHash;
        return this;
    }

    public String getSeqOriginalHash() {
        return seqOriginalHash;
    }

    public NextstrainGenbankHashes setSeqOriginalHash(String seqOriginalHash) {
        this.seqOriginalHash = seqOriginalHash;
        return this;
    }
}
