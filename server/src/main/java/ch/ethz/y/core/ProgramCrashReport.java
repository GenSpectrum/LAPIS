package ch.ethz.y.core;


public class ProgramCrashReport implements SendableReport {

    private final Throwable e;
    private final String programName;


    public ProgramCrashReport(Throwable e, String programName) {
        this.e = e;
        this.programName = programName;
    }


    public Throwable getE() {
        return e;
    }


    @Override
    public PriorityLevel getPriority() {
        return PriorityLevel.FATAL;
    }

    @Override
    public String getProgramName() {
        return programName;
    }


    @Override
    public String getEmailText() {
        return "The program crashed!\n\n" + Utils.getStackTraceString(e);
    }
}
