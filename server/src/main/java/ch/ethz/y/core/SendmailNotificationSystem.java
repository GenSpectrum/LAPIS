package ch.ethz.y.core;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * This notification system uses the "sendmail" command of the operating system. It expect it to be fully configured
 * with no further authentication needed. The idea was discarded when it was understood that within the Singularity
 * container on Euler, accessing sendmail is not directly possible.
 *
 * It was never tested/used!!
 */
public class SendmailNotificationSystem implements NotificationSystem {
    private final List<String> recipients;

    public SendmailNotificationSystem(List<String> recipients) {
        this.recipients = recipients;
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

        try {
            Process process = Runtime.getRuntime().exec ("sendmail -t");
            OutputStream out = process.getOutputStream();
            PrintWriter p = new PrintWriter(out);
            p.print(emailSourceCode);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
