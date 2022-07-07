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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;


@SpringBootApplication
@EnableScheduling
public class LapisMain extends SubProgram<LapisConfig> {

    public static LapisConfig globalConfig;
    public static ComboPooledDataSource dbPool;

    public LapisMain() {
        super("Lapis", LapisConfig.class);
    }

    public void exportAllSeqs(ComboPooledDataSource dbPool) throws SQLException, IOException {
        BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream("sequences.fasta.xz"));
        XZOutputStream xzOut = new XZOutputStream(fileOut, new LZMA2Options());
        SeqCompressor referenceSeqCompressor = new ZstdSeqCompressor(DICT.REFERENCE);

        String sql = """
            select gisaid_epi_isl, seq_original_compressed
            from y_gisaid
            where seq_original_compressed is not null;
            """;
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
                        xzOut.write(">".getBytes(StandardCharsets.UTF_8));
                        xzOut.write(rs.getString("gisaid_epi_isl").getBytes(StandardCharsets.UTF_8));
                        xzOut.write("\n".getBytes(StandardCharsets.UTF_8));
                        xzOut.write(referenceSeqCompressor.decompress(rs.getBytes("seq_original_compressed"))
                            .getBytes(StandardCharsets.UTF_8));
                        xzOut.write("\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        } finally {
            xzOut.close();
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

        exportAllSeqs(dbPool);
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
