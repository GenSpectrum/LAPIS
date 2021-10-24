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
}
