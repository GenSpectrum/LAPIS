package ch.ethz.y.core;


/**
 * This one does nothing and can be used as placeholder when no notification is desired.
 */
public class NoopNotificationSystem implements NotificationSystem {
    @Override
    public void sendReport(SendableReport report) {

    }
}
