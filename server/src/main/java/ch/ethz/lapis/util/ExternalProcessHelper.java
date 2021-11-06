package ch.ethz.lapis.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExternalProcessHelper {
    public static Process exec(String command, int timeoutMinutes) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            boolean exited = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!exited) {
                process.destroyForcibly();
                throw new RuntimeException("External process timeout: " + command);
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("External process exited with code " + process.exitValue() + ": " + command);
            }
            return process;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Process execNextalign(
        Path outputPath,
        Path nextalignPath,
        Path seqFastaPath,
        Path referenceFasta,
        Path geneMapGff
    ) {
        List<String> genes = ReferenceGenomeData.getInstance().getGeneNames();
        String command = nextalignPath.toAbsolutePath() +
            " --sequences=" + seqFastaPath.toAbsolutePath() +
            " --reference=" + referenceFasta.toAbsolutePath() +
            " --genemap=" + geneMapGff.toAbsolutePath() +
            " --genes=" + String.join(",", genes) +
            " --output-dir=" + outputPath.toAbsolutePath() +
            " --output-basename=nextalign" +
            " --silent" +
            " --jobs=1";
        return ExternalProcessHelper.exec(command, 20);
    }

    public static Process executePangolin(Path outputPath, Path seqFastaPath) {
        String command = "conda run --no-capture-output -n pangolin pangolin -o " + outputPath.toAbsolutePath() +
            " " + seqFastaPath.toAbsolutePath();
        return ExternalProcessHelper.exec(command, 20);
    }

    public static Process executePangolinUsher(Path outputPath, Path seqFastaPath) {
        String command = "conda run --no-capture-output -n pangolin pangolin --usher " +
            "--outfile lineage_report-usher.csv -o " + outputPath.toAbsolutePath() + " " +
            seqFastaPath.toAbsolutePath();
        return ExternalProcessHelper.exec(command, 20);
    }

    public static Process executePangolinVersion(Path outputPath) {
        String command = "conda run --no-capture-output -n pangolin pangolin --all-versions > " +
            outputPath.toAbsolutePath();
        return ExternalProcessHelper.exec(command, 20);
    }

    public static Process executePangolinUpdate() {
        String command = "conda run --no-capture-output -n pangolin pangolin --update";
        return ExternalProcessHelper.exec(command, 20);
    }
}
