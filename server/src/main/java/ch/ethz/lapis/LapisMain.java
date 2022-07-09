package ch.ethz.lapis;

import ch.ethz.lapis.core.DatabaseService;
import ch.ethz.lapis.core.GlobalProxyManager;
import ch.ethz.lapis.core.SubProgram;
import ch.ethz.lapis.source.covlineages.CovLineagesService;
import ch.ethz.lapis.source.gisaid.GisaidService;
import ch.ethz.lapis.source.ng.NextstrainGenbankService;
import ch.ethz.lapis.source.s3c.S3CVineyardService;
import ch.ethz.lapis.transform.TransformService;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor.DICT;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Euler commands to export all nuc and aa sequences

// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, seq_original_compressed as seq_compressed from y_gisaid where seq_original_compressed is not null;" nuc output/sequences.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, seq_aligned_compressed as seq_compressed from y_gisaid where seq_aligned_compressed is not null;" nuc output/aligned.fasta
//
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'E';" aa output/gene_E.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'N';" aa output/gene_N.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'M';" aa output/gene_M.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF7a';" aa output/gene_ORF7a.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF8';" aa output/gene_ORF8.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF6';" aa output/gene_ORF6.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF3a';" aa output/gene_ORF3a.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF9b';" aa output/gene_ORF9b.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF7b';" aa output/gene_ORF7b.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'S';" aa output/gene_S.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF1a';" aa output/gene_ORF1a.fasta
// bsub -N -n 1 -R "rusage[mem=8000]" -W 24:00 java -Xmx8g -jar lapis.jar --config ./config.yml Lapis "select gisaid_epi_isl, aa_seq_compressed as seq_compressed from y_main_aa_sequence a join y_main_metadata m on a.id = m.id where aa_seq_compressed is not null and gene = 'ORF1b';" aa output/gene_ORF1b.fasta

// Compress them
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_E.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_M.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_N.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF1a.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF1b.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF3a.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF6.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF7a.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF7b.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF8.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_ORF9b.fasta
// bsub -N -n 1 -R "rusage[mem=500]" -W 4:00 xz -k output/gene_S.fasta
//
// bsub -N -n 8 -R "rusage[mem=500]" -W 24:00 xz -vkT8 output/aligned.fasta
// bsub -N -n 8 -R "rusage[mem=500]" -W 24:00 xz -vkT8 output/sequences.fasta

@SpringBootApplication
@EnableScheduling
public class LapisMain extends SubProgram<LapisConfig> {

    public static LapisConfig globalConfig;
    public static ComboPooledDataSource dbPool;

    public LapisMain() {
        super("Lapis", LapisConfig.class);
    }

    public void exportAllSeqs(
        ComboPooledDataSource dbPool,
        String sql,
        String type,
        String fileOutName
    ) throws SQLException, IOException {
        BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(fileOutName));
//        XZOutputStream xzOut = new XZOutputStream(fileOut, new LZMA2Options());
        SeqCompressor seqCompressor = switch (type) {
            case "aa" ->  new ZstdSeqCompressor(DICT.AA_REFERENCE);
            case "nuc" -> new ZstdSeqCompressor(DICT.REFERENCE);
            default -> throw new RuntimeException("Expected type: " + type);
        };

//        String sql = """
//            select gisaid_epi_isl, seq_original_compressed as seq_compressed
//            from y_gisaid
//            where seq_original_compressed is not null;
//            """;
        try (Connection conn = dbPool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.setFetchSize(10000);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 100000 == 0) {
                            System.out.println(LocalDateTime.now() + " " + (i / 100000));
                        }
                        i++;
                        fileOut.write(">".getBytes(StandardCharsets.UTF_8));
                        fileOut.write(rs.getString("gisaid_epi_isl").getBytes(StandardCharsets.UTF_8));
                        fileOut.write("\n".getBytes(StandardCharsets.UTF_8));
                        fileOut.write(seqCompressor.decompress(rs.getBytes("seq_compressed"))
                            .replace("\n", "")
                            .getBytes(StandardCharsets.UTF_8));
                        fileOut.write("\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        } finally {
//            xzOut.close();
            fileOut.close();
        }
    }

    @Override
    public void run(String[] args, LapisConfig config) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("TODO: write help page");
        }
        globalConfig = config;
        dbPool = DatabaseService.createDatabaseConnectionPool(LapisMain.globalConfig.getVineyard());

        System.out.println("Query: " + args[0]);
        System.out.println("Type: " + args[1]);
        System.out.println("File: " + args[2]);
        exportAllSeqs(dbPool, args[0], args[1], args[2]);
        System.exit(0);

        GlobalProxyManager.setProxyFromConfig(config.getHttpProxy());
        if ("--api".equals(args[0])) {
            String[] argsForSpring = Arrays.copyOfRange(args, 1, args.length);
            System.setProperty("spring.mvc.async.request-timeout", "600000"); // 10 minutes
            SpringApplication app = new SpringApplication(LapisMain.class);
            app.setDefaultProperties(Collections.singletonMap("server.port", "2345"));
            app.run(argsForSpring);
            return;
        }
        if ("--update-data".equals(args[0])) {
            if (args.length < 2) {
                throw new RuntimeException("Please provide the update steps. - TODO: write help page");
            }
            String[] updateSteps = args[1].split(",");
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            Set<String> availableSteps = new HashSet<>() {{
                add(UpdateSteps.loadNG);
                add(UpdateSteps.loadGisaid);
                add(UpdateSteps.loadGisaidMissingSubmitters);
                add(UpdateSteps.loadS3C);
                add(UpdateSteps.loadPangolinAssignment);
                add(UpdateSteps.transformNG);
                add(UpdateSteps.transformGisaid);
                add(UpdateSteps.mergeFromS3C);
                add(UpdateSteps.mergeFromPangolinAssignment);
                add(UpdateSteps.finalTransforms);
                add(UpdateSteps.switchInStaging);
            }};
            for (String updateStep : updateSteps) {
                if (!availableSteps.contains(updateStep)) {
                    throw new RuntimeException("Unknown update step: " + updateStep);
                }
            }
            for (String updateStep : updateSteps) {
                switch (updateStep) {
                    case UpdateSteps.loadNG -> new NextstrainGenbankService(
                        dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextalignPath()
                    ).updateData();
                    case UpdateSteps.loadGisaid -> new GisaidService(
                        dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextcladePath(),
                        config.getGisaidApiConfig(), config.getGeoLocationRulesPath(), config.getNotificationKey()
                    ).updateData();
                    case UpdateSteps.loadGisaidMissingSubmitters -> new GisaidService(
                        dbPool, config.getWorkdir(), config.getMaxNumberWorkers(), config.getNextcladePath(),
                        config.getGisaidApiConfig(), config.getGeoLocationRulesPath(), config.getNotificationKey()
                    ).fetchMissingSubmitterInformation();
                    case UpdateSteps.loadS3C -> new S3CVineyardService(dbPool, config.getS3cVineyard()).updateData();
                    case UpdateSteps.loadPangolinAssignment -> new CovLineagesService(dbPool, config.getWorkdir())
                        .pullGisaidPangoLineageAssignments();
                    case UpdateSteps.transformNG -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAndTransform(LapisConfig.Source.NG);
                    case UpdateSteps.transformGisaid -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAndTransform(LapisConfig.Source.GISAID);
                    case UpdateSteps.mergeFromS3C -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAdditionalMetadataFromS3c();
                    case UpdateSteps.mergeFromPangolinAssignment ->
                        new TransformService(dbPool, config.getMaxNumberWorkers()).mergeFromPangolinAssignment();
                    case UpdateSteps.finalTransforms ->
                        new TransformService(dbPool, config.getMaxNumberWorkers()).finalTransforms();
                    case UpdateSteps.switchInStaging -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .switchInStagingTables();
                }
            }
        }
    }

}
