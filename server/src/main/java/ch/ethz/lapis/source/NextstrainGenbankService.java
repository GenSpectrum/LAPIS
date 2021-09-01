package ch.ethz.lapis.source;

import ch.ethz.lapis.core.ExhaustibleBlockingQueue;
import ch.ethz.lapis.core.ExhaustibleLinkedBlockingQueue;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jooq.SQL;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;


public class NextstrainGenbankService {

    private final ComboPooledDataSource databasePool;
    private final Path workdir;
    private final int maxNumberWorkers;
    private final Path nextalignPath;
    private final SeqCompressor seqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);

    public NextstrainGenbankService(
            ComboPooledDataSource databasePool,
            String workdir,
            int maxNumberWorkers,
            String nextalignPath
    ) {
        this.databasePool = databasePool;
        this.workdir = Path.of(workdir);
        this.maxNumberWorkers = maxNumberWorkers;
        this.nextalignPath = Path.of(nextalignPath);
    }

    public void updateData() throws IOException, SQLException, InterruptedException {
        // Download files from Nextstrain
        downloadFiles();

        // Delete old data
        deleteOldData();

        // The files and different types of data will be inserted/updated independently and one after the other in the
        // following order:
        //   1. original sequences
        //   2. aligned sequences
        //   3. metadata
        //   4. nextclade data (for now, only the aa mutations)

        // It is highly important to update the AAMutations before the aligned sequences because the AAMutations check
        // the old aligned sequences and only compute the aa mutation sequences if the aligned sequences changed.
        updateAAMutations();
        updateSeqOriginalOrAligned(false);
        updateSeqOriginalOrAligned(true);
        updateMetadata();
        updateNextcladeData();
    }


    private void downloadFiles() throws IOException {
        // Downloading the following files from data.nextstrain.org/files/ncov/open/
        //     metadata.tsv.gz
        //     sequences.fasta.xz
        //     aligned.fasta.xz
        //     nextclade.tsv.gz
        String urlPrefix = "https://data.nextstrain.org/files/ncov/open/";
        List<String> files = new ArrayList<>() {{
            add("metadata.tsv.gz");
            add("sequences.fasta.xz");
            add("aligned.fasta.xz");
            add("nextclade.tsv.gz");
        }};

        for (String file : files) {
            URL url = new URL(urlPrefix + file);
            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(workdir.resolve(file).toFile());
            fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }


    private void deleteOldData() throws SQLException {
        String sql = "truncate y_nextstrain_genbank;";
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        }
    }


    private void updateSeqOriginalOrAligned(boolean aligned) throws IOException, SQLException {
        String filename = !aligned ? "sequences.fasta.xz" : "aligned.fasta.xz";
        String sql = """
            insert into y_nextstrain_genbank (strain, seq_original_compressed)
            values (?, ?)
            on conflict (strain) do update
            set seq_original_compressed = ?;
        """;
        if (aligned) {
            sql = sql.replaceAll("_original_", "_aligned_");
        }
        int i = 0;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (FastaFileReader fastaReader = new FastaFileReader(workdir.resolve(filename), true)) {
                    for (FastaEntry entry : fastaReader) {
                        byte[] compressed = seqCompressor.compress(aligned ? entry.getSeq().toUpperCase() : entry.getSeq());
                        statement.setString(1, entry.getSampleName());
                        statement.setBytes(2, compressed);
                        statement.setBytes(3, compressed);
                        statement.addBatch();
                        i++;
                        if (i % 10000 == 0) {
                            Utils.executeClearCommitBatch(conn, statement);
                        }
                    }
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }

    }


    private void updateAAMutations() throws IOException, SQLException, InterruptedException {
        // TODO Check if Nextalign is installed

        // Write the reference sequence and genemap.gff to the workdir
        Path referenceFasta = workdir.resolve("reference.fasta");
        Files.writeString(referenceFasta, ">REFERENCE\n" + Utils.getReferenceSeq() + "\n\n");
        Path geneMapGff = workdir.resolve("genemap.gff");
        Files.writeString(geneMapGff, Utils.getGeneMapGff());

        // Create a queue to store batches and start workers to process them.
        ExhaustibleBlockingQueue<List<FastaEntry>> batchQueue
                = new ExhaustibleLinkedBlockingQueue<>(Math.max(4, maxNumberWorkers / 2));
        final ConcurrentLinkedQueue<Exception> unhandledExceptions = new ConcurrentLinkedQueue<>();
        final AtomicBoolean emergencyBrake = new AtomicBoolean(false);
        ExecutorService executor = Executors.newFixedThreadPool(maxNumberWorkers);
        for (int i = 0; i < maxNumberWorkers; i++) {
            //Create a work directory for the worker
            Path workerWorkDir = workdir.resolve("worker-" + i);
            Files.createDirectory(workerWorkDir);

            // Start worker
            final int finalI = i;
            executor.submit(() -> {
                var worker = new NextstrainGenbankMutationAAWorker(
                        finalI,
                        databasePool,
                        workerWorkDir,
                        referenceFasta,
                        geneMapGff,
                        nextalignPath
                );
                while (!emergencyBrake.get() && (!batchQueue.isExhausted() || !batchQueue.isEmpty())) {
                    try {
                        List<FastaEntry> batch = batchQueue.poll(5, TimeUnit.SECONDS);
                        if (batch == null) {
                            continue;
                        }
                        worker.run(batch);
                    } catch (InterruptedException e) {
                        // When the emergency brake is pulled, it is likely that a worker will be interrupted. This is
                        // normal and does not constitute an additional error.
                        if (!emergencyBrake.get()) {
                            unhandledExceptions.add(e);
                        }
                    } catch (Exception e) {
                        unhandledExceptions.add(e);
                        emergencyBrake.set(true);
                        return;
                    }
                }
            });
        }

        // Read file
        String filename = "aligned.fasta.xz";
        int batchSize = 1000;
        List<FastaEntry> batch = new ArrayList<>();
        try (FastaFileReader fastaReader = new FastaFileReader(workdir.resolve(filename), true)) {
            for (FastaEntry entry : fastaReader) {
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    while (!emergencyBrake.get()) {
                        System.out.println("[main] Try adding a batch");
                        boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                        if (success) {
                            System.out.println("[main] Batch added");
                            break;
                        }
                    }
                    batch = new ArrayList<>();
                }
            }
            if (!emergencyBrake.get() && !batch.isEmpty()) {
                while (!emergencyBrake.get()) {
                    System.out.println("[main] Try adding the last batch");
                    boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                    if (success) {
                        System.out.println("[main] Batch added");
                        break;
                    }
                }
                batch = null;
            }
            batchQueue.setExhausted(true);
        }

        // If someone pulled the emergency brake, collect some information and send a notification email.
        if (emergencyBrake.get()) {
            System.err.println("Emergency exit!");
            System.err.println("The sequence batch processing workers are reporting unhandled errors:");
            for (Exception unhandledException : unhandledExceptions) {
                unhandledException.printStackTrace();
            }
            executor.shutdown();
            boolean terminated = executor.awaitTermination(3, TimeUnit.MINUTES);
            if (!terminated) {
                executor.shutdownNow();
            }
        } else {
            // Wait until all batches are finished.
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
    }


    private void updateMetadata() throws IOException, SQLException {
        InputStream fileInputStream = new FileInputStream(workdir.resolve("metadata.tsv.gz").toFile());
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);

        String sql = """
            insert into y_nextstrain_genbank (
              genbank_accession, sra_accession, gisaid_epi_isl, strain, date, date_original,
              date_submitted, region, country, division, location, region_exposure, country_exposure,
              division_exposure, host, age, sex, sampling_strategy, pango_lineage, nextstrain_clade,
              gisaid_clade, originating_lab, submitting_lab, authors
            )
            values (
              ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?
            )
            on conflict (strain) do update
            set
              genbank_accession = ?,
              sra_accession = ?,
              gisaid_epi_isl = ?,
              date = ?,
              date_original = ?,
              date_submitted = ?,
              region = ?,
              country = ?,
              division = ?,
              location = ?,
              region_exposure = ?,
              country_exposure = ?,
              division_exposure = ?,
              host = ?,
              age = ?,
              sex = ?,
              sampling_strategy = ?,
              pango_lineage = ?,
              nextstrain_clade = ?,
              gisaid_clade = ?,
              originating_lab = ?,
              submitting_lab = ?,
              authors = ?;
        """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (NextstrainGenbankMetadataFileReader metadataReader
                             = new NextstrainGenbankMetadataFileReader(gzipInputStream)) {
                    int i = 0;
                    for (NextstrainGenbankMetadataEntry entry : metadataReader) {
                        if (entry.getStrain() == null) {
                            continue;
                        }
                        statement.setString(1, entry.getGenbankAccession());
                        statement.setString(2, entry.getSraAccession());
                        statement.setString(3, entry.getGisaidEpiIsl());
                        statement.setString(4, entry.getStrain());
                        statement.setDate(5, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setString(6, entry.getDateOriginal());
                        statement.setDate(7, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(8, entry.getRegion());
                        statement.setString(9, entry.getCountry());
                        statement.setString(10, entry.getDivision());
                        statement.setString(11, entry.getLocation());
                        statement.setString(12, entry.getRegionExposure());
                        statement.setString(13, entry.getCountryExposure());
                        statement.setString(14, entry.getDivisionExposure());
                        statement.setString(15, entry.getHost());
                        statement.setObject(16, entry.getAge());
                        statement.setString(17, entry.getSex());
                        statement.setString(18, entry.getSamplingStrategy());
                        statement.setString(19, entry.getPangoLineage());
                        statement.setString(20, entry.getNextstrainClade());
                        statement.setString(21, entry.getGisaidClade());
                        statement.setString(22, entry.getOriginatingLab());
                        statement.setString(23, entry.getSubmittingLab());
                        statement.setString(24, entry.getAuthors());

                        statement.setString(25, entry.getGenbankAccession());
                        statement.setString(26, entry.getSraAccession());
                        statement.setString(27, entry.getGisaidEpiIsl());
                        statement.setDate(28, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setString(29, entry.getDateOriginal());
                        statement.setDate(30, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(31, entry.getRegion());
                        statement.setString(32, entry.getCountry());
                        statement.setString(33, entry.getDivision());
                        statement.setString(34, entry.getLocation());
                        statement.setString(35, entry.getRegionExposure());
                        statement.setString(36, entry.getCountryExposure());
                        statement.setString(37, entry.getDivisionExposure());
                        statement.setString(38, entry.getHost());
                        statement.setObject(39, entry.getAge());
                        statement.setString(40, entry.getSex());
                        statement.setString(41, entry.getSamplingStrategy());
                        statement.setString(42, entry.getPangoLineage());
                        statement.setString(43, entry.getNextstrainClade());
                        statement.setString(44, entry.getGisaidClade());
                        statement.setString(45, entry.getOriginatingLab());
                        statement.setString(46, entry.getSubmittingLab());
                        statement.setString(47, entry.getAuthors());

                        statement.addBatch();
                        if (i++ % 10000 == 0) {
                            Utils.executeClearCommitBatch(conn, statement);
                        }
                    }
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }
    }


    private void updateNextcladeData() throws IOException, SQLException {
        InputStream fileInputStream = new FileInputStream(workdir.resolve("nextclade.tsv.gz").toFile());
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);

        String sql = """
            insert into y_nextstrain_genbank (strain, aa_mutations, nuc_substitutions, nuc_deletions, nuc_insertions)
            values (?, ?, ?, ?, ?)
            on conflict (strain) do update
            set
              aa_mutations = ?,
              nuc_substitutions = ?,
              nuc_deletions = ?,
              nuc_insertions = ?;
        """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (NextstrainGenbankNextcladeFileReader nextcladeReader
                             = new NextstrainGenbankNextcladeFileReader(gzipInputStream)) {
                    int i = 0;
                    for (NextstrainGenbankNextcladeEntry entry : nextcladeReader) {
                        statement.setString(1, entry.getStrain());
                        statement.setString(2, entry.getAaMutations());
                        statement.setString(3, entry.getNucSubstitutions());
                        statement.setString(4, entry.getNucDeletions());
                        statement.setString(5, entry.getNucInsertions());

                        statement.setString(6, entry.getAaMutations());
                        statement.setString(7, entry.getNucSubstitutions());
                        statement.setString(8, entry.getNucDeletions());
                        statement.setString(9, entry.getNucInsertions());

                        statement.addBatch();
                        if (i++ % 10000 == 0) {
                            Utils.executeClearCommitBatch(conn, statement);
                        }
                    }
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }
    }

}
