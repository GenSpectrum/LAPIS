package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.core.Utils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FinalReport {

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

    public enum PriorityLevel {
        FATAL,      // Immediate action required: the program can not continue working and might cause corruptions.
        ERROR,      // Immediate action required: the program can not continue working.
        WARNING,    // Something seems off but the program is able to continue.
        INFO       // Everything is okay.
    }

    public PriorityLevel getPriority() {
        if (!success) {
            return PriorityLevel.FATAL;
        } else {
            return PriorityLevel.INFO;
        }
    }

    public String getEmailText() {
        StringBuilder text =
            new StringBuilder("Success: " + success + "\n\n" +
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
                "Number of unhandled exceptions: " + unhandledExceptions.size() + "\n\n");
        if (!unhandledExceptions.isEmpty()) {
            text.append("Unhandled exceptions:\n");
            for (Exception unhandledException : unhandledExceptions) {
                text.append(Utils.getStackTraceString(unhandledException)).append("\n");
            }
            text.append("\n\n");
        }
        return text.toString();
    }

    public FinalReport setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public FinalReport setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public FinalReport setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public FinalReport setEntriesInDataPackage(int entriesInDataPackage) {
        this.entriesInDataPackage = entriesInDataPackage;
        return this;
    }

    public FinalReport setProcessedEntries(int processedEntries) {
        this.processedEntries = processedEntries;
        return this;
    }

    public FinalReport setAddedEntries(int addedEntries) {
        this.addedEntries = addedEntries;
        return this;
    }

    public FinalReport setUpdatedTotalEntries(int updatedTotalEntries) {
        this.updatedTotalEntries = updatedTotalEntries;
        return this;
    }

    public FinalReport setUpdatedMetadataEntries(int updatedMetadataEntries) {
        this.updatedMetadataEntries = updatedMetadataEntries;
        return this;
    }

    public FinalReport setUpdatedSequenceEntries(int updatedSequenceEntries) {
        this.updatedSequenceEntries = updatedSequenceEntries;
        return this;
    }

    public FinalReport setDeletedEntries(int deletedEntries) {
        this.deletedEntries = deletedEntries;
        return this;
    }

    public FinalReport setFailedEntries(int failedEntries) {
        this.failedEntries = failedEntries;
        return this;
    }

    public FinalReport setUnhandledExceptions(List<Exception> unhandledExceptions) {
        this.unhandledExceptions = unhandledExceptions;
        return this;
    }
}
