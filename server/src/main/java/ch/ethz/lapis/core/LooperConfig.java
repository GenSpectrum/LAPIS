package ch.ethz.lapis.core;

public class LooperConfig implements Config {

    private Boolean activated;
    private Integer minutesBetweenRuns;

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public Integer getMinutesBetweenRuns() {
        return minutesBetweenRuns;
    }

    public void setMinutesBetweenRuns(Integer minutesBetweenRuns) {
        this.minutesBetweenRuns = minutesBetweenRuns;
    }
}
