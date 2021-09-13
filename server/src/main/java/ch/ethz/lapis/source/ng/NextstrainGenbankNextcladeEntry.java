package ch.ethz.lapis.source.ng;

public class NextstrainGenbankNextcladeEntry {
    private String strain;
    private String aaMutations;
    private String nucSubstitutions;
    private String nucDeletions;
    private String nucInsertions;

    public String getStrain() {
        return strain;
    }

    public NextstrainGenbankNextcladeEntry setStrain(String strain) {
        this.strain = strain;
        return this;
    }

    public String getAaMutations() {
        return aaMutations;
    }

    public NextstrainGenbankNextcladeEntry setAaMutations(String aaMutations) {
        this.aaMutations = aaMutations;
        return this;
    }

    public String getNucSubstitutions() {
        return nucSubstitutions;
    }

    public NextstrainGenbankNextcladeEntry setNucSubstitutions(String nucSubstitutions) {
        this.nucSubstitutions = nucSubstitutions;
        return this;
    }

    public String getNucDeletions() {
        return nucDeletions;
    }

    public NextstrainGenbankNextcladeEntry setNucDeletions(String nucDeletions) {
        this.nucDeletions = nucDeletions;
        return this;
    }

    public String getNucInsertions() {
        return nucInsertions;
    }

    public NextstrainGenbankNextcladeEntry setNucInsertions(String nucInsertions) {
        this.nucInsertions = nucInsertions;
        return this;
    }
}
