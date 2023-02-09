package ch.ethz.lapis.source;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.core.ConfigurationManager;
import ch.ethz.lapis.core.DatabaseService;
import ch.ethz.lapis.source.ng.NextstrainFileToDatabaseService;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TestDataGenerator {

    public static final int LIMIT = 20000;
    public static final String ORIGIN_DIR = "../../dataWorkdir";
    public static final String TESTDATA_DIR = ORIGIN_DIR + "/testdata";

//    @Test // run this manually - this depends on already downloaded open data in ORIGIN_DIR
    void extractRkiTestdataFromFullDataset() throws Exception {
        if (!new File(TESTDATA_DIR).exists()) {
            new File(TESTDATA_DIR).mkdir();
        }

        HashSet<String> rkiEntries = metadata();
        nextclade(rkiEntries);
        sequences(rkiEntries, "sequences.fasta.xz");
        sequences(rkiEntries, "aligned.fasta.xz");
    }

//    @Test // run this manually - this depends on the output of extractRkiTestdataFromFullDataset
    void putTestdataIntoLocalDatabase() throws Exception {
        LapisConfig lapisConfig = new ConfigurationManager().loadConfiguration(Path.of("config.local.yml"));

        new NextstrainFileToDatabaseService(
            DatabaseService.createDatabaseConnectionPool(lapisConfig.getVineyard()),
            Path.of(TESTDATA_DIR),
            3,
            Path.of(lapisConfig.getNextalignPath())
        ).insertFilesIntoDatabase(LocalDateTime.now());
    }

    private HashSet<String> metadata() throws IOException {
        File metadataFile = Path.of(ORIGIN_DIR + "/metadata.tsv.gz").toFile();

        BufferedReader reader = getGzipInputReader(metadataFile);

        var rkiEntries = new HashSet<String>();
        try (var outStream = getGzipOutStream(new File(TESTDATA_DIR + "/metadata.tsv.gz"))) {
            outStream.write((reader.readLine() + "\n").getBytes());

            String line;
            var start = System.currentTimeMillis();
            var i = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("rki")) {
                    continue;
                }
                i++;
                outStream.write((line + "\n").getBytes());
                rkiEntries.add(line.split("\t")[0]);
                if (i > LIMIT) {
                    break;
                }
            }
            System.out.println(
                "Collected " + rkiEntries.size() + " RKI entries in " + (System.currentTimeMillis() - start) + "ms"
            );
        }
        reader.close();
        return rkiEntries;
    }

    private void nextclade(HashSet<String> rkiEntries) throws Exception {
        File nextcladeFile = Path.of(ORIGIN_DIR + "/nextclade.tsv.gz").toFile();

        BufferedReader reader = getGzipInputReader(nextcladeFile);

        try (var outStream = getGzipOutStream(new File(TESTDATA_DIR + "/nextclade.tsv.gz"))) {
            outStream.write((reader.readLine() + "\n").getBytes());

            var start = System.currentTimeMillis();
            String line;
            while ((line = reader.readLine()) != null) {
                var strain = line.split("\t")[1];
                if (!rkiEntries.contains(strain)) {
                    continue;
                }

                outStream.write((line + "\n").getBytes());
            }
            System.out.println("Collected nextclade data in " + (System.currentTimeMillis() - start) + "ms");
        }
        reader.close();
    }

    private GZIPOutputStream getGzipOutStream(File targetFile) throws IOException {
        return new GZIPOutputStream(new FileOutputStream(targetFile));
    }

    private BufferedReader getGzipInputReader(File metadataFile) throws IOException {
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(metadataFile))));
    }

    private void sequences(HashSet<String> rkiEntries, String fileName) throws Exception {
        File file = Path.of(ORIGIN_DIR + "/" + fileName).toFile();
        var reader = new BufferedReader(new InputStreamReader(new XZInputStream(new FileInputStream(file))));

        File targetFile = new File(TESTDATA_DIR + "/" + fileName);
        try (var outStream = new XZOutputStream(new FileOutputStream(targetFile), new LZMA2Options())) {
            String line;
            var start = System.currentTimeMillis();
            var hits = 0;

            fastForwardReaderToWhereRkiSequenceRoughlyAre(reader);

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(">")) {
                    continue;
                }
                var strain = line.substring(1);
                if (!rkiEntries.contains(strain)) {
                    continue;
                }
                hits++;
                outStream.write((line + "\n").getBytes());
                outStream.write((reader.readLine() + "\n").getBytes());

                if (hits > LIMIT) {
                    break;
                }
            }
            System.out.println("Collected sequences from " + fileName + " in " + (System.currentTimeMillis() - start) + "ms");
        }
        reader.close();
    }

    private void fastForwardReaderToWhereRkiSequenceRoughlyAre(BufferedReader reader) throws IOException {
        for (int j = 0; j < 1000000; j++) {
            reader.readLine();
        }
    }
}
