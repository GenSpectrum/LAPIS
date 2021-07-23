package ch.ethz.y.core;

import java.nio.file.Path;

public class NotificationSystemFactory {
    public NotificationSystem createNotificationSystemFromConfig(NotificationConfig config) {
        if (!config.getActivated()) {
            return new NoopNotificationSystem();
        }
        switch (config.getType()) {
            case "smtp":
                NotificationConfig.SenderConfig senderConfig = config.getSender();
                return new SmtpNotificationSystem(
                        senderConfig.getSmtpHost(),
                        senderConfig.getSmtpPort(),
                        senderConfig.getSmtpUsername(),
                        senderConfig.getSmtpPassword(),
                        senderConfig.getAddress(),
                        config.getRecipients()
                );
            case "sendmail":
                return new SendmailNotificationSystem(config.getRecipients());
            case "dropoff":
                return new DropOffNotificationSystem(config.getRecipients(), Path.of(config.getDropoffDirectory()));
        }
        throw new RuntimeException("Unknown notification type: " + config.getType());
    }
}
