package ch.ethz.lapis.source.gisaid;


public class BatchReport {

    private int addedEntries;
    private int updatedTotalEntries;
    private int updatedMetadataEntries;
    private int updatedSequenceEntries;
    private int failedEntries;


    public int getAddedEntries() {
        return addedEntries;
    }

    public BatchReport setAddedEntries(int addedEntries) {
        this.addedEntries = addedEntries;
        return this;
    }

    public int getUpdatedTotalEntries() {
        return updatedTotalEntries;
    }

    public BatchReport setUpdatedTotalEntries(int updatedTotalEntries) {
        this.updatedTotalEntries = updatedTotalEntries;
        return this;
    }

    public int getUpdatedMetadataEntries() {
        return updatedMetadataEntries;
    }

    public BatchReport setUpdatedMetadataEntries(int updatedMetadataEntries) {
        this.updatedMetadataEntries = updatedMetadataEntries;
        return this;
    }

    public int getUpdatedSequenceEntries() {
        return updatedSequenceEntries;
    }

    public BatchReport setUpdatedSequenceEntries(int updatedSequenceEntries) {
        this.updatedSequenceEntries = updatedSequenceEntries;
        return this;
    }

    public int getFailedEntries() {
        return failedEntries;
    }

    public BatchReport setFailedEntries(int failedEntries) {
        this.failedEntries = failedEntries;
        return this;
    }


    @Override
    public String toString() {
        return "BatchReport{" +
                "addedEntries=" + addedEntries +
                ", updatedEntries=" + updatedMetadataEntries +
                ", failedEntries=" + failedEntries +
                '}';
    }
}
