package ch.ethz.lapis;

public class UpdateSteps {

    public static final String loadNG = "load-nextstrain-genbank";
    public static final String loadGisaid = "load-gisaid";
    public static final String loadGisaidMissingSubmitters = "load-gisaid-missing-submitters";
    public static final String loadS3C = "load-s3c";
    public static final String loadPangolinAssignment = "load-pangolin-assignment";
    public static final String transformNG = "transform-nextstrain-genbank";
    public static final String transformGisaid = "transform-gisaid";
    public static final String mergeFromS3C = "merge-from-s3c";
    public static final String mergeFromPangolinAssignment = "merge-form-pangolin-assignment";
    public static final String finalTransforms = "final-transforms";
    public static final String switchInStaging = "switch-in-staging";

    public static final String loadMpox = "load-mpox";
    public static final String transformMpox = "mergeFromMpox";

}
