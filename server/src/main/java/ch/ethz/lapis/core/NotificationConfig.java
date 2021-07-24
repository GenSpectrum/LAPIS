package ch.ethz.lapis.core;

import java.util.List;

public class NotificationConfig implements Config {

    public static class SenderConfig implements Config {
        private String smtpHost;
        private Integer smtpPort;
        private String smtpUsername;
        private String smtpPassword;
        private String address;

        public String getSmtpHost() {
            return smtpHost;
        }

        public SenderConfig setSmtpHost(String smtpHost) {
            this.smtpHost = smtpHost;
            return this;
        }

        public Integer getSmtpPort() {
            return smtpPort;
        }

        public SenderConfig setSmtpPort(Integer smtpPort) {
            this.smtpPort = smtpPort;
            return this;
        }

        public String getSmtpUsername() {
            return smtpUsername;
        }

        public SenderConfig setSmtpUsername(String smtpUsername) {
            this.smtpUsername = smtpUsername;
            return this;
        }

        public String getSmtpPassword() {
            return smtpPassword;
        }

        public SenderConfig setSmtpPassword(String smtpPassword) {
            this.smtpPassword = smtpPassword;
            return this;
        }

        public String getAddress() {
            return address;
        }

        public SenderConfig setAddress(String address) {
            this.address = address;
            return this;
        }
    }

    private Boolean activated;
    private String type;
    private String dropoffDirectory;
    private List<String> recipients;
    private SenderConfig sender;

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDropoffDirectory() {
        return dropoffDirectory;
    }

    public void setDropoffDirectory(String dropoffDirectory) {
        this.dropoffDirectory = dropoffDirectory;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public SenderConfig getSender() {
        return sender;
    }

    public NotificationConfig setSender(SenderConfig sender) {
        this.sender = sender;
        return this;
    }
}
