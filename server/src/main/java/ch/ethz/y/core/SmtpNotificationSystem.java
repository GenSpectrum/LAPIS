package ch.ethz.y.core;


import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.util.ArrayList;
import java.util.List;

public class SmtpNotificationSystem implements NotificationSystem, SendAttachmentCapable {

    private final String senderAddress;
    private final List<String> recipients;
    private final Mailer mailer;


    public SmtpNotificationSystem(
            String senderSmtpHost,
            int senderSmtpPort,
            String senderSmtpUsername,
            String senderSmtpPassword,
            String senderAddress,
            List<String> recipients
    ) {
        this.senderAddress = senderAddress;
        this.recipients = recipients;

        mailer = MailerBuilder
                .withSMTPServer(senderSmtpHost, senderSmtpPort, senderSmtpUsername, senderSmtpPassword)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .buildMailer();
    }


    @Override
    public void sendReport(SendableReport report) {
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(senderAddress)
                .withSubject(report.getSubject())
                .withPlainText(report.getEmailText());
        for (String recipient : recipients) {
            builder.to(recipient);
        }
        if (report.getPriority() == SendableReport.PriorityLevel.FATAL) {
            builder.withHeader("X-Priority", 1);
        }
        Email email = builder.buildEmail();
        mailer.sendMail(email);
    }

    @Override
    public void sendReportWithAttachment(SendableReportWithAttachments report) {
        sendReportWithAttachment(report, new ArrayList<>());
    }

    @Override
    public void sendReportWithAttachment(SendableReportWithAttachments report, List<String> additionalRecipients) {
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(senderAddress)
                .withSubject(report.getSubject())
                .withPlainText(report.getEmailText());
        for (String recipient : recipients) {
            builder.to(recipient);
        }
        for (String recipient : additionalRecipients) {
            builder.to(recipient);
        }
        if (report.getPriority() == SendableReport.PriorityLevel.FATAL) {
            builder.withHeader("X-Priority", 1);
        }
        for (ReportAttachment attachment : report.getAttachments()) {
            builder.withAttachment(attachment.getAttachmentFilename(), attachment.getBytes(), attachment.getMimeType());
        }
        Email email = builder.buildEmail();
        mailer.sendMail(email);
    }
}
