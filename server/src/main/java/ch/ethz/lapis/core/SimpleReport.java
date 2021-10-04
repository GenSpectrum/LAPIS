package ch.ethz.lapis.core;


public class SimpleReport implements SendableReport {

    private final PriorityLevel priorityLevel;
    private final String text;
    private final String programName;

    public SimpleReport(String text, String programName) {
        this(text, programName, PriorityLevel.INFO);
    }

    public SimpleReport(String text, String programName, PriorityLevel priorityLevel) {
        this.programName = programName;
        this.priorityLevel = priorityLevel;
        this.text = text;
    }

    @Override
    public PriorityLevel getPriority() {
        return priorityLevel;
    }

    @Override
    public String getProgramName() {
        return programName;
    }

    @Override
    public String getEmailText() {
        return text;
    }
}
