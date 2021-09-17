package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.core.SendableReport;
import ch.ethz.lapis.core.Utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * The report that will be sent to via email
 */
public class FinalReport implements SendableReport {

    private boolean success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int entriesInDataPackage;
    private int processedEntries;
    private int addedEntries;
    private int updatedTotalEntries;
    private int updatedMetadataEntries;
    private int updatedSequenceEntries;
    private int deletedEntries;
    private int failedEntries;
    private List<Exception> unhandledExceptions = new ArrayList<>();

    @Override
    public PriorityLevel getPriority() {
        if (!success) {
            return PriorityLevel.FATAL;
        } else {
            return PriorityLevel.INFO;
        }
    }

    @Override
    public String getProgramName() {
        return "LAPIS GisaidService";
    }

    @Override
    public String getEmailText() {
        String text =
                "Success: " + success + "\n\n" +
                        "Started at: " + startTime + "\n" +
                        "Ended at: " + endTime + "\n\n" +
                        "Failed entries: " + failedEntries + "\n\n" +
                        "Entries in the data package: " + entriesInDataPackage + "\n" +
                        "Processed entries: " + processedEntries + "\n" +
                        "Added entries: " + addedEntries + "\n" +
                        "Updated entries: " + updatedTotalEntries + "\n" +
                        "Updated metadata: " + updatedMetadataEntries + "\n" +
                        "Updated sequences: " + updatedSequenceEntries + "\n" +
                        "Deleted entries: " + deletedEntries + "\n" +
                        "Number of unhandled exceptions: " + unhandledExceptions.size() + "\n\n";
        if (!unhandledExceptions.isEmpty()) {
            text += "Unhandled exceptions:\n";
            for (Exception unhandledException : unhandledExceptions) {
                text += Utils.getStackTraceString(unhandledException) + "\n";
            }
            text += "\n\n";
        }
        return text;
    }


    public boolean isSuccess() {
        return success;
    }

    public FinalReport setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public FinalReport setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public FinalReport setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public int getEntriesInDataPackage() {
        return entriesInDataPackage;
    }

    public FinalReport setEntriesInDataPackage(int entriesInDataPackage) {
        this.entriesInDataPackage = entriesInDataPackage;
        return this;
    }

    public int getProcessedEntries() {
        return processedEntries;
    }

    public FinalReport setProcessedEntries(int processedEntries) {
        this.processedEntries = processedEntries;
        return this;
    }

    public int getAddedEntries() {
        return addedEntries;
    }

    public FinalReport setAddedEntries(int addedEntries) {
        this.addedEntries = addedEntries;
        return this;
    }

    public int getUpdatedTotalEntries() {
        return updatedTotalEntries;
    }

    public FinalReport setUpdatedTotalEntries(int updatedTotalEntries) {
        this.updatedTotalEntries = updatedTotalEntries;
        return this;
    }

    public int getUpdatedMetadataEntries() {
        return updatedMetadataEntries;
    }

    public FinalReport setUpdatedMetadataEntries(int updatedMetadataEntries) {
        this.updatedMetadataEntries = updatedMetadataEntries;
        return this;
    }

    public int getUpdatedSequenceEntries() {
        return updatedSequenceEntries;
    }

    public FinalReport setUpdatedSequenceEntries(int updatedSequenceEntries) {
        this.updatedSequenceEntries = updatedSequenceEntries;
        return this;
    }

    public int getDeletedEntries() {
        return deletedEntries;
    }

    public FinalReport setDeletedEntries(int deletedEntries) {
        this.deletedEntries = deletedEntries;
        return this;
    }

    public int getFailedEntries() {
        return failedEntries;
    }

    public FinalReport setFailedEntries(int failedEntries) {
        this.failedEntries = failedEntries;
        return this;
    }

    public List<Exception> getUnhandledExceptions() {
        return unhandledExceptions;
    }

    public FinalReport setUnhandledExceptions(List<Exception> unhandledExceptions) {
        this.unhandledExceptions = unhandledExceptions;
        return this;
    }
}
