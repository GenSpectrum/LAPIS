package ch.ethz.lapis.core;

public class Looper {

    private final LooperConfig config;
    private boolean first = true;

    public Looper(LooperConfig config) {
        this.config = config;
    }

    public boolean next() {
        if (first) {
            first = false;
            return true;
        }
        return isActivated();
    }

    public void sleep() throws InterruptedException {
        if (isActivated()) {
            Thread.sleep(config.getMinutesBetweenRuns() * 60 * 1000);
        }
    }

    private boolean isActivated() {
        return config != null && config.getActivated() != null && config.getActivated();
    }
}
