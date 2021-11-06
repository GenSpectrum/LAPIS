package ch.ethz.lapis.transform;

import ch.ethz.lapis.util.ExternalProcessHelper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PangoLineageComputer {

    public static void updatePangolin() {
        ExternalProcessHelper.executePangolinUpdate();
    }

    public static String getPangoAllVersions(Path outputPath) {
        Path fileOutputPath = outputPath.resolve("pango-version.txt");
        ExternalProcessHelper.executePangolinVersion(fileOutputPath);
        try {
            return Files.readString(fileOutputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PangoLineageEntry> computePangoLineage(Path outputPath, Path seqFastaPath) {
        // Run pangolin
        ExternalProcessHelper.executePangolin(outputPath, seqFastaPath);
        // Disabling the UShER mode for now because it is quite slow.
//        ExternalProcessHelper.executePangolinUsher(outputPath, seqFastaPath);

        // Read results
        Map<String, PangoLineageEntry> lineages = new HashMap<>();
        try {
            // Default
            Path pangolinDefaultReport = outputPath.resolve("lineage_report.csv");
            readPangoLineageReport(lineages, pangolinDefaultReport, false);

            // Usher
//            Path pangolinUsherReport = outputPath.resolve("lineage_report-usher.csv");
//            readPangoLineageReport(lineages, pangolinUsherReport, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(lineages.values());
    }

    private static void readPangoLineageReport(
        Map<String, PangoLineageEntry> lineages,
        Path pangolinDefaultResults,
        boolean isUsher
    ) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .parse(new FileReader(pangolinDefaultResults.toFile()));
        for (CSVRecord record : records) {
            String sequenceName = record.get("taxon");
            String lineage = record.get("lineage");
            if ("None".equals(lineage)) {
                lineage = null;
            }
            lineages.putIfAbsent(sequenceName, new PangoLineageEntry(sequenceName));
            if (!isUsher) {
                lineages.get(sequenceName).setPangoLineage(lineage);
            } else {
                lineages.get(sequenceName).setPangoLineageUsher(lineage);
            }
        }
    }

}
