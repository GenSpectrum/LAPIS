package ch.ethz.lapis.core;


public interface SendableReport {

    enum PriorityLevel {
        FATAL,      // Immediate action required: the program can not continue working and might caused corruptions.
        ERROR,      // Immediate action required: the program can not continue working.
        WARNING,    // Something seems off but the program is able to continue.
        INFO;       // Everything is okay.
    }

    PriorityLevel getPriority();

    String getProgramName();

    default String getSubject() {
        return "[Harvester] " + getProgramName() + " - " + getPriority();
    }

    String getEmailText();
}
