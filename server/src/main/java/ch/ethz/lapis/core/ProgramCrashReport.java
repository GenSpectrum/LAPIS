package ch.ethz.lapis.core;


public record ProgramCrashReport(Throwable e, String programName) implements SendableReport {
    @Override
    public PriorityLevel getPriority() {
        return PriorityLevel.FATAL;
    }


    @Override
    public String getEmailText() {
        return "The program crashed!\n\n" + Utils.getStackTraceString(e);
    }
}
