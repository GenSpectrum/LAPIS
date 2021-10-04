package ch.ethz.lapis.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * This system writes the messages formatted as an email to a local directories.
 */
public class DropOffNotificationSystem implements NotificationSystem {

    private final List<String> recipients;
    private final Path dropOffDirectory;

    public DropOffNotificationSystem(List<String> recipients, Path dropOffDirectory) {
        this.recipients = recipients;
        this.dropOffDirectory = dropOffDirectory;
    }

    @Override
    public void sendReport(SendableReport report) {
        String subject = "[Harvester] GISAID API Import - " + report.getPriority();

        String emailSourceCode = "To: " + String.join(",", recipients) + "\n"
            + "Subject: " + subject + "\n"
            + "Content-Type: text/plain\n";
        if (report.getPriority() == SendableReport.PriorityLevel.FATAL) {
            emailSourceCode += "X-Priority: 1 (Highest)\n";
        }
        emailSourceCode += "\n" + report.getEmailText() + "\n";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileName = LocalDateTime.now().format(formatter)
            + "_" + System.nanoTime()
            + ".txt";
        try {
            Files.writeString(dropOffDirectory.resolve(fileName), emailSourceCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
