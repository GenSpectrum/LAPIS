package ch.ethz.lapis;

import ch.ethz.lapis.core.ConfigurationManager;
import ch.ethz.lapis.core.DatabaseService;
import ch.ethz.lapis.core.GlobalProxyManager;
import ch.ethz.lapis.source.covlineages.CovLineagesService;
import ch.ethz.lapis.source.gisaid.GisaidService;
import ch.ethz.lapis.source.ng.NextstrainGenbankService;
import ch.ethz.lapis.transform.TransformService;
import ch.ethz.lapis.util.Encryptor;
import ch.ethz.lapis.util.Utils;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class LapisMain {

    public static LapisConfig globalConfig;
    public static ComboPooledDataSource dbPool;
    public static ComboPooledDataSource extDbPool;
    public static Encryptor encryptor;

    public static void main(String[] args) throws Exception {
        ConfigurationManager configurationManager = new ConfigurationManager();
        if (args.length < 2 || !args[0].equals("--config")) {
            System.out.println("Please run with '--config <path/to/config>'");
            System.exit(1);
        }

        LapisConfig lapisConfig = configurationManager.loadConfiguration(Path.of(args[1]));

        (new LapisMain()).run(Arrays.copyOfRange(args, 2, args.length), lapisConfig);
    }

    public void loadBag(String passphrase) throws Exception {
        String rScriptPath = "/Users/chachen/Documents/repos/GenSpectrum/foph-import/main.R";
        String command = "Rscript " + rScriptPath + " " + passphrase;
        Process process = Runtime.getRuntime().exec(command);
        String inputDataJson = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String error = new String(process.getErrorStream().readAllBytes());
        boolean exited = process.waitFor(5, TimeUnit.MINUTES);
        if (!exited) {
            throw new RuntimeException("R script exited with code " + process.exitValue() + ". Message: " + error);
        }

        String deleteSql = "delete from bag;";
        String insertSql = """
            insert into bag (gisaid_epi_isl, additional_data)
            values (?, ?)
            on conflict do nothing;
            """;

        JSONArray entries = new JSONArray(inputDataJson);
        try (Connection conn = dbPool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.execute(deleteSql);
            }
            try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
                for (Object entry : entries) {
                    JSONObject obj = (JSONObject) entry;
                    String entryGisaidId = obj.getString("gisaid_id");
                    String entryJson = obj.toString();
                    String entryEncrypted = encryptor.encrypt(entryJson);
                    statement.setString(1, entryGisaidId);
                    statement.setString(2, entryEncrypted);
                    statement.addBatch();
                }
                Utils.executeClearCommitBatch(conn, statement);
            }
            conn.setAutoCommit(true);
        }

        System.out.println("Done");
    }

    public void loadFromExternalLapis() throws Exception {
        String truncateSql = "truncate y_gisaid;";
        String selectSql = """
            select *
            from y_gisaid
            where country = 'Switzerland';
            """;
        String insertSql = """
            insert into y_gisaid (
              updated_at, metadata_hash, seq_original_hash, gisaid_epi_isl, strain, date, year,month, day,
              date_original, date_submitted, region, country, division, location, region_exposure, country_exposure,
              division_exposure, host, age, sex, sampling_strategy, pango_lineage, gisaid_clade, originating_lab,
              submitting_lab, authors, seq_original_compressed, seq_aligned_compressed, aa_seqs_compressed,
              aa_mutations, aa_insertions, aa_unknowns, nuc_substitutions, nuc_deletions, nuc_insertions, nuc_unknowns,
              nextclade_clade, nextclade_clade_long, nextclade_pango_lineage, nextclade_total_substitutions,
              nextclade_total_deletions, nextclade_total_insertions, nextclade_total_frame_shifts,
              nextclade_total_aminoacid_substitutions, nextclade_total_aminoacid_deletions,
              nextclade_total_aminoacid_insertions, nextclade_total_missing, nextclade_total_non_acgtns,
              nextclade_total_pcr_primer_changes, nextclade_pcr_primer_changes, nextclade_alignment_score,
              nextclade_alignment_start, nextclade_alignment_end, nextclade_qc_overall_score,
              nextclade_qc_overall_status, nextclade_qc_missing_data_missing_data_threshold,
              nextclade_qc_missing_data_score, nextclade_qc_missing_data_status,
              nextclade_qc_missing_data_total_missing, nextclade_qc_mixed_sites_mixed_sites_threshold,
              nextclade_qc_mixed_sites_score, nextclade_qc_mixed_sites_status,
              nextclade_qc_mixed_sites_total_mixed_sites, nextclade_qc_private_mutations_cutoff,
              nextclade_qc_private_mutations_excess, nextclade_qc_private_mutations_score,
              nextclade_qc_private_mutations_status, nextclade_qc_private_mutations_total,
              nextclade_qc_snp_clusters_clustered_snps, nextclade_qc_snp_clusters_score,
              nextclade_qc_snp_clusters_status, nextclade_qc_snp_clusters_total_snps,
              nextclade_qc_frame_shifts_frame_shifts, nextclade_qc_frame_shifts_total_frame_shifts,
              nextclade_qc_frame_shifts_frame_shifts_ignored, nextclade_qc_frame_shifts_total_frame_shifts_ignored,
              nextclade_qc_frame_shifts_score, nextclade_qc_frame_shifts_status, nextclade_qc_stop_codons_stop_codons,
              nextclade_qc_stop_codons_total_stop_codons, nextclade_qc_stop_codons_score,
              nextclade_qc_stop_codons_status, nextclade_coverage, nextclade_errors
            ) values (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?
            );
            """;

        try (Connection extConn = extDbPool.getConnection(); Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(truncateSql);
            }

            conn.setAutoCommit(false);
            try (Statement selectStatement = extConn.createStatement();
                PreparedStatement insertStatement = conn.prepareStatement(insertSql)) {
                try (ResultSet rs = selectStatement.executeQuery(selectSql)) {
                    while (rs.next()) {
                        insertStatement.setObject(1, rs.getObject("updated_at"));
                        insertStatement.setObject(2, rs.getObject("metadata_hash"));
                        insertStatement.setObject(3, rs.getObject("seq_original_hash"));
                        insertStatement.setObject(4, rs.getObject("gisaid_epi_isl"));
                        insertStatement.setObject(5, rs.getObject("strain"));
                        insertStatement.setObject(6, rs.getObject("date"));
                        insertStatement.setObject(7, rs.getObject("year"));
                        insertStatement.setObject(8, rs.getObject("month"));
                        insertStatement.setObject(9, rs.getObject("day"));
                        insertStatement.setObject(10, rs.getObject("date_original"));
                        insertStatement.setObject(11, rs.getObject("date_submitted"));
                        insertStatement.setObject(12, rs.getObject("region"));
                        insertStatement.setObject(13, rs.getObject("country"));
                        insertStatement.setObject(14, rs.getObject("division"));
                        insertStatement.setObject(15, rs.getObject("location"));
                        insertStatement.setObject(16, rs.getObject("region_exposure"));
                        insertStatement.setObject(17, rs.getObject("country_exposure"));
                        insertStatement.setObject(18, rs.getObject("division_exposure"));
                        insertStatement.setObject(19, rs.getObject("host"));
                        insertStatement.setObject(20, rs.getObject("age"));
                        insertStatement.setObject(21, rs.getObject("sex"));
                        insertStatement.setObject(22, rs.getObject("sampling_strategy"));
                        insertStatement.setObject(23, rs.getObject("pango_lineage"));
                        insertStatement.setObject(24, rs.getObject("gisaid_clade"));
                        insertStatement.setObject(25, rs.getObject("originating_lab"));
                        insertStatement.setObject(26, rs.getObject("submitting_lab"));
                        insertStatement.setObject(27, rs.getObject("authors"));
                        insertStatement.setObject(28, rs.getObject("seq_original_compressed"));
                        insertStatement.setObject(29, rs.getObject("seq_aligned_compressed"));
                        insertStatement.setObject(30, rs.getObject("aa_seqs_compressed"));
                        insertStatement.setObject(31, rs.getObject("aa_mutations"));
                        insertStatement.setObject(32, rs.getObject("aa_insertions"));
                        insertStatement.setObject(33, rs.getObject("aa_unknowns"));
                        insertStatement.setObject(34, rs.getObject("nuc_substitutions"));
                        insertStatement.setObject(35, rs.getObject("nuc_deletions"));
                        insertStatement.setObject(36, rs.getObject("nuc_insertions"));
                        insertStatement.setObject(37, rs.getObject("nuc_unknowns"));
                        insertStatement.setObject(38, rs.getObject("nextclade_clade"));
                        insertStatement.setObject(39, rs.getObject("nextclade_clade_long"));
                        insertStatement.setObject(40, rs.getObject("nextclade_pango_lineage"));
                        insertStatement.setObject(41, rs.getObject("nextclade_total_substitutions"));
                        insertStatement.setObject(42, rs.getObject("nextclade_total_deletions"));
                        insertStatement.setObject(43, rs.getObject("nextclade_total_insertions"));
                        insertStatement.setObject(44, rs.getObject("nextclade_total_frame_shifts"));
                        insertStatement.setObject(45, rs.getObject("nextclade_total_aminoacid_substitutions"));
                        insertStatement.setObject(46, rs.getObject("nextclade_total_aminoacid_deletions"));
                        insertStatement.setObject(47, rs.getObject("nextclade_total_aminoacid_insertions"));
                        insertStatement.setObject(48, rs.getObject("nextclade_total_missing"));
                        insertStatement.setObject(49, rs.getObject("nextclade_total_non_acgtns"));
                        insertStatement.setObject(50, rs.getObject("nextclade_total_pcr_primer_changes"));
                        insertStatement.setObject(51, rs.getObject("nextclade_pcr_primer_changes"));
                        insertStatement.setObject(52, rs.getObject("nextclade_alignment_score"));
                        insertStatement.setObject(53, rs.getObject("nextclade_alignment_start"));
                        insertStatement.setObject(54, rs.getObject("nextclade_alignment_end"));
                        insertStatement.setObject(55, rs.getObject("nextclade_qc_overall_score"));
                        insertStatement.setObject(56, rs.getObject("nextclade_qc_overall_status"));
                        insertStatement.setObject(57, rs.getObject("nextclade_qc_missing_data_missing_data_threshold"));
                        insertStatement.setObject(58, rs.getObject("nextclade_qc_missing_data_score"));
                        insertStatement.setObject(59, rs.getObject("nextclade_qc_missing_data_status"));
                        insertStatement.setObject(60, rs.getObject("nextclade_qc_missing_data_total_missing"));
                        insertStatement.setObject(61, rs.getObject("nextclade_qc_mixed_sites_mixed_sites_threshold"));
                        insertStatement.setObject(62, rs.getObject("nextclade_qc_mixed_sites_score"));
                        insertStatement.setObject(63, rs.getObject("nextclade_qc_mixed_sites_status"));
                        insertStatement.setObject(64, rs.getObject("nextclade_qc_mixed_sites_total_mixed_sites"));
                        insertStatement.setObject(65, rs.getObject("nextclade_qc_private_mutations_cutoff"));
                        insertStatement.setObject(66, rs.getObject("nextclade_qc_private_mutations_excess"));
                        insertStatement.setObject(67, rs.getObject("nextclade_qc_private_mutations_score"));
                        insertStatement.setObject(68, rs.getObject("nextclade_qc_private_mutations_status"));
                        insertStatement.setObject(69, rs.getObject("nextclade_qc_private_mutations_total"));
                        insertStatement.setObject(70, rs.getObject("nextclade_qc_snp_clusters_clustered_snps"));
                        insertStatement.setObject(71, rs.getObject("nextclade_qc_snp_clusters_score"));
                        insertStatement.setObject(72, rs.getObject("nextclade_qc_snp_clusters_status"));
                        insertStatement.setObject(73, rs.getObject("nextclade_qc_snp_clusters_total_snps"));
                        insertStatement.setObject(74, rs.getObject("nextclade_qc_frame_shifts_frame_shifts"));
                        insertStatement.setObject(75, rs.getObject("nextclade_qc_frame_shifts_total_frame_shifts"));
                        insertStatement.setObject(76, rs.getObject("nextclade_qc_frame_shifts_frame_shifts_ignored"));
                        insertStatement.setObject(77, rs.getObject("nextclade_qc_frame_shifts_total_frame_shifts_ignored"));
                        insertStatement.setObject(78, rs.getObject("nextclade_qc_frame_shifts_score"));
                        insertStatement.setObject(79, rs.getObject("nextclade_qc_frame_shifts_status"));
                        insertStatement.setObject(80, rs.getObject("nextclade_qc_stop_codons_stop_codons"));
                        insertStatement.setObject(81, rs.getObject("nextclade_qc_stop_codons_total_stop_codons"));
                        insertStatement.setObject(82, rs.getObject("nextclade_qc_stop_codons_score"));
                        insertStatement.setObject(83, rs.getObject("nextclade_qc_stop_codons_status"));
                        insertStatement.setObject(84, rs.getObject("nextclade_coverage"));
                        insertStatement.setObject(85, rs.getObject("nextclade_errors"));
                        insertStatement.addBatch();
                    }
                    Utils.executeClearCommitBatch(conn, insertStatement);
                }
            }
            conn.setAutoCommit(true);
        }
    }

    public void run(String[] args, LapisConfig config) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("TODO: write help page");
        }
        globalConfig = config;
        dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
        GlobalProxyManager.setProxyFromConfig(config.getHttpProxy());

        Scanner scanner = new Scanner(System.in);
        String passphrase = scanner.nextLine();
        encryptor = new Encryptor(passphrase, config.getSalt());

        if ("--api".equals(args[0])) {
            String[] argsForSpring = Arrays.copyOfRange(args, 1, args.length);
            System.setProperty("spring.mvc.async.request-timeout", "3600000"); // 60 minutes
            SpringApplication app = new SpringApplication(LapisMain.class);
            app.setDefaultProperties(Collections.singletonMap("server.port", "2345"));
            app.run(argsForSpring);
            return;
        }
        if ("--update-data".equals(args[0])) {
            if (args.length < 2) {
                throw new RuntimeException("Please provide the update steps. - TODO: write help page");
            }
            extDbPool = DatabaseService.createDatabaseConnectionPool(config.getExtVineyard());
            String[] updateSteps = args[1].split(",");
            ComboPooledDataSource dbPool = DatabaseService.createDatabaseConnectionPool(config.getVineyard());
            Set<String> availableSteps = new HashSet<>() {{
                add(UpdateSteps.loadNG);
                add(UpdateSteps.loadGisaid);
                add(UpdateSteps.loadGisaidMissingSubmitters);
                add(UpdateSteps.loadBag);
                add(UpdateSteps.loadPangolinAssignment);
                add(UpdateSteps.transformNG);
                add(UpdateSteps.transformGisaid);
                add(UpdateSteps.mergeFromBag);
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
                    case UpdateSteps.loadBag -> {
                        loadBag(passphrase);
                        loadFromExternalLapis();
                    }
                    case UpdateSteps.loadPangolinAssignment -> new CovLineagesService(dbPool, config.getWorkdir())
                        .pullGisaidPangoLineageAssignments();
                    case UpdateSteps.transformNG -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAndTransform(LapisConfig.Source.NG);
                    case UpdateSteps.transformGisaid -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAndTransform(LapisConfig.Source.GISAID);
                    case UpdateSteps.mergeFromBag -> new TransformService(dbPool, config.getMaxNumberWorkers())
                        .mergeAdditionalMetadataFromBag();
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
