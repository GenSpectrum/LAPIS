package ch.ethz.lapis.source.ng;

import ch.ethz.lapis.core.ExhaustibleBlockingQueue;
import ch.ethz.lapis.core.ExhaustibleLinkedBlockingQueue;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
    private final SeqCompressor seqCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE);
    private Map<String, NextstrainGenbankHashes> oldHashes;
    // updateSeqOriginalOrAligned(false) will fill this set with the "strains" of the entries for which the original
    // sequence did not change. For those sequences, we will not overwrite the AA mutations and Nextclade data either.
    private final Set<String> unchangedOriginalSeqStrains = new HashSet<>();

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
        LocalDateTime startTime = LocalDateTime.now();

        // Download files from Nextstrain
        downloadFiles();

        // Load the hashes of the existing data
        this.oldHashes = getHashes();

        // The files and different types of data will be inserted/updated independently and one after the other in the
        // following order:
        //   1. original sequences
        //   2. aligned sequences
        //   3. AA mutations (self-computed with Nextalign)
        //   4. metadata
        //   5. nextclade data (provided by the data source)

        updateSeqOriginalOrAligned(false);
        updateSeqOriginalOrAligned(true);
        updateAAMutations();
        updateMetadata();
        updateNextcladeData();

        updateDataVersion(startTime);
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


    private Map<String, NextstrainGenbankHashes> getHashes() throws SQLException {
        String sql = """
                select strain, metadata_hash, seq_original_hash, seq_aligned_hash
                from y_nextstrain_genbank;
            """;
        Map<String, NextstrainGenbankHashes> hashes = new HashMap<>();
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    while (rs.next()) {
                        hashes.put(
                            rs.getString("strain"),
                            new NextstrainGenbankHashes(
                                rs.getString("metadata_hash"),
                                rs.getString("seq_original_hash"),
                                rs.getString("seq_aligned_hash")
                            )
                        );
                    }
                }
            }
        }
        return hashes;
    }


    private void updateSeqOriginalOrAligned(boolean aligned) throws IOException, SQLException {
        String filename = !aligned ? "sequences.fasta.xz" : "aligned.fasta.xz";
        String sql = """
                insert into y_nextstrain_genbank (strain, seq_original_compressed, seq_original_hash)
                values (?, ?, ?)
                on conflict (strain) do update
                set
                  seq_original_compressed = ?,
                  seq_original_hash = ?;
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
                        String seq = aligned ? entry.getSeq().toUpperCase() : entry.getSeq();
                        String sampleName = entry.getSampleName();
                        String currentHash = Utils.hashMd5(seq);
                        if (oldHashes.containsKey(sampleName)) {
                            String oldHash = aligned ? oldHashes.get(sampleName).getSeqAlignedHash() :
                                oldHashes.get(sampleName).getSeqOriginalHash();
                            if (currentHash.equals(oldHash)) {
                                if (!aligned) {
                                    unchangedOriginalSeqStrains.add(sampleName);
                                }
                                continue;
                            }
                        }
                        byte[] compressed = seqCompressor.compress(seq);
                        statement.setString(1, sampleName);
                        statement.setBytes(2, compressed);
                        statement.setString(3, currentHash);
                        statement.setBytes(4, compressed);
                        statement.setString(5, currentHash);
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


    /**
     * This function will also compute and write aa_unknowns and nuc_unknowns.
     */
    private void updateAAMutations() throws IOException, InterruptedException {
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
                if (unchangedOriginalSeqStrains.contains(entry.getSampleName())) {
                    continue;
                }
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    while (!emergencyBrake.get()) {
                        System.out.println(LocalDateTime.now() + " [main] Try adding a batch");
                        boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                        if (success) {
                            System.out.println(LocalDateTime.now() + " [main] Batch added");
                            break;
                        }
                    }
                    batch = new ArrayList<>();
                }
            }
            if (!emergencyBrake.get() && !batch.isEmpty()) {
                while (!emergencyBrake.get()) {
                    System.out.println(LocalDateTime.now() + " [main] Try adding the last batch");
                    boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                    if (success) {
                        System.out.println(LocalDateTime.now() + " [main] Batch added");
                        break;
                    }
                }
                batch = null;
            }
            batchQueue.setExhausted(true);
        }

        // If someone pulled the emergency brake, collect some information and send a notification email.
        if (emergencyBrake.get()) {
            System.err.println(LocalDateTime.now() + " Emergency exit!");
            System.err.println(LocalDateTime.now() + " The sequence batch processing workers are reporting unhandled errors:");
            for (Exception unhandledException : unhandledExceptions) {
                unhandledException.printStackTrace();
            }
            executor.shutdown();
            boolean terminated = executor.awaitTermination(3, TimeUnit.MINUTES);
            if (!terminated) {
                executor.shutdownNow();
            }
            throw new RuntimeException("NextstrainGenbankService updateAAMutations() failed");
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
                  genbank_accession, sra_accession, gisaid_epi_isl, strain, date, year, month, day, date_original,
                  date_submitted, region, country, division, location, region_exposure, country_exposure,
                  division_exposure, host, age, sex, sampling_strategy, pango_lineage, nextstrain_clade,
                  gisaid_clade, originating_lab, submitting_lab, authors, metadata_hash
                )
                values (
                  ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?
                )
                on conflict (strain) do update
                set
                  genbank_accession = ?,
                  sra_accession = ?,
                  gisaid_epi_isl = ?,
                  date = ?,
                  year = ?,
                  month = ?,
                  day = ?,
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
                  authors = ?,
                  metadata_hash = ?;
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
                        String sampleName = entry.getStrain();
                        String currentHash = Utils.hashMd5(entry);
                        if (oldHashes.containsKey(sampleName)) {
                            String oldHash = oldHashes.get(sampleName).getMetadataHash();
                            if (currentHash.equals(oldHash)) {
                                continue;
                            }
                        }

                        statement.setString(1, entry.getGenbankAccession());
                        statement.setString(2, entry.getSraAccession());
                        statement.setString(3, entry.getGisaidEpiIsl());
                        statement.setString(4, entry.getStrain());
                        statement.setDate(5, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setObject(6, entry.getYear());
                        statement.setObject(7, entry.getMonth());
                        statement.setObject(8, entry.getDay());
                        statement.setString(9, entry.getDateOriginal());
                        statement.setDate(10, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(11, entry.getRegion());
                        statement.setString(12, entry.getCountry());
                        statement.setString(13, entry.getDivision());
                        statement.setString(14, entry.getLocation());
                        statement.setString(15, entry.getRegionExposure());
                        statement.setString(16, entry.getCountryExposure());
                        statement.setString(17, entry.getDivisionExposure());
                        statement.setString(18, entry.getHost());
                        statement.setObject(19, entry.getAge());
                        statement.setString(20, entry.getSex());
                        statement.setString(21, entry.getSamplingStrategy());
                        statement.setString(22, entry.getPangoLineage());
                        statement.setString(23, entry.getNextstrainClade());
                        statement.setString(24, entry.getGisaidClade());
                        statement.setString(25, entry.getOriginatingLab());
                        statement.setString(26, entry.getSubmittingLab());
                        statement.setString(27, entry.getAuthors());
                        statement.setString(28, currentHash);

                        statement.setString(29, entry.getGenbankAccession());
                        statement.setString(30, entry.getSraAccession());
                        statement.setString(31, entry.getGisaidEpiIsl());
                        statement.setDate(32, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setObject(33, entry.getYear());
                        statement.setObject(34, entry.getMonth());
                        statement.setObject(35, entry.getDay());
                        statement.setString(36, entry.getDateOriginal());
                        statement.setDate(37, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(38, entry.getRegion());
                        statement.setString(39, entry.getCountry());
                        statement.setString(40, entry.getDivision());
                        statement.setString(41, entry.getLocation());
                        statement.setString(42, entry.getRegionExposure());
                        statement.setString(46, entry.getCountryExposure());
                        statement.setString(44, entry.getDivisionExposure());
                        statement.setString(45, entry.getHost());
                        statement.setObject(46, entry.getAge());
                        statement.setString(47, entry.getSex());
                        statement.setString(48, entry.getSamplingStrategy());
                        statement.setString(49, entry.getPangoLineage());
                        statement.setString(50, entry.getNextstrainClade());
                        statement.setString(51, entry.getGisaidClade());
                        statement.setString(52, entry.getOriginatingLab());
                        statement.setString(53, entry.getSubmittingLab());
                        statement.setString(54, entry.getAuthors());
                        statement.setString(55, currentHash);

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
                        if (unchangedOriginalSeqStrains.contains(entry.getStrain())) {
                            continue;
                        }
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


    private void updateDataVersion(LocalDateTime startTime) throws SQLException {
        ZoneId zoneId = ZoneId.systemDefault();
        long epoch = startTime.atZone(zoneId).toEpochSecond();
        String sql = """
                insert into data_version (dataset, timestamp)
                values ('nextstrain-genbank', ?)
                on conflict (dataset) do update
                set
                  timestamp = ?;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setLong(1, epoch);
                statement.setLong(2, epoch);
                statement.execute();
            }
        }
    }

}
