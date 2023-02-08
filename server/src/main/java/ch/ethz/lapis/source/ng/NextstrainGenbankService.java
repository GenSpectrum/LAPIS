package ch.ethz.lapis.source.ng;

import ch.ethz.lapis.core.ExhaustibleBlockingQueue;
import ch.ethz.lapis.core.ExhaustibleLinkedBlockingQueue;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
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

        downloadFilesFromNextstrain();

        this.oldHashes = getHashesOfExistingData();

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


    private void downloadFilesFromNextstrain() throws IOException {
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


    private Map<String, NextstrainGenbankHashes> getHashesOfExistingData() throws SQLException {
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
        log.info("starting updateSeqOriginalOrAligned for aligned: " + aligned);

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
                        String seq = aligned ? entry.sequence().toUpperCase() : entry.sequence();
                        String sampleName = entry.sampleName();
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
                            log.info("executing batch in updateSeqOriginalOrAligned for fasta entry i=" + i);
                            Utils.executeClearCommitBatch(conn, statement);
                        }
                        if (i > 500000) {
                            break;
                        }
                    }
                    Utils.executeClearCommitBatch(conn, statement);
                }
            }
            conn.setAutoCommit(true);
        }
        log.info("finished updateSeqOriginalOrAligned");
    }


    /**
     * This function will also compute and write aa_unknowns and nuc_unknowns.
     */
    private void updateAAMutations() throws IOException, InterruptedException {
        log.info("starting updateAAMutations");
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
                if (unchangedOriginalSeqStrains.contains(entry.sampleName())) {
                    continue;
                }
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    while (!emergencyBrake.get()) {
                        log.info(LocalDateTime.now() + " [main] Try adding a batch");
                        boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                        if (success) {
                            log.info(LocalDateTime.now() + " [main] Batch added");
                            break;
                        }
                    }
                    batch = new ArrayList<>();
                }
            }
            if (!emergencyBrake.get() && !batch.isEmpty()) {
                while (!emergencyBrake.get()) {
                    log.info(LocalDateTime.now() + " [main] Try adding the last batch");
                    boolean success = batchQueue.offer(batch, 5, TimeUnit.SECONDS);
                    if (success) {
                        log.info(LocalDateTime.now() + " [main] Batch added");
                        break;
                    }
                }
                batch = null;
            }
            batchQueue.setExhausted(true);
        }

        // If someone pulled the emergency brake, collect some information and send a notification email.
        if (emergencyBrake.get()) {
            log.error("Emergency exit! The sequence batch processing workers are reporting unhandled errors:");
            for (Exception unhandledException : unhandledExceptions) {
                log.error(unhandledException.getMessage(), unhandledException);
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
        log.info("finished updateAAMutations");
    }


    private void updateMetadata() throws IOException, SQLException {
        log.info("starting updateMetadata");

        InputStream fileInputStream = new FileInputStream(workdir.resolve("metadata.tsv.gz").toFile());
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);

        String sql = """
                insert into y_nextstrain_genbank (
                  genbank_accession, sra_accession, gisaid_epi_isl, strain, date, year, month, day, date_original,
                  date_submitted, region, country, division, location, region_exposure, country_exposure,
                  division_exposure, host, age, sex, sampling_strategy, pango_lineage, nextclade_pango_lineage,
                  nextstrain_clade, gisaid_clade, originating_lab, submitting_lab, authors, metadata_hash
                )
                values (
                  ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?
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
                  nextclade_pango_lineage = ?,
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
                        statement.setString(23, entry.getNextcladePangoLineage());
                        statement.setString(24, entry.getNextstrainClade());
                        statement.setString(25, entry.getGisaidClade());
                        statement.setString(26, entry.getOriginatingLab());
                        statement.setString(27, entry.getSubmittingLab());
                        statement.setString(28, entry.getAuthors());
                        statement.setString(29, currentHash);

                        statement.setString(30, entry.getGenbankAccession());
                        statement.setString(31, entry.getSraAccession());
                        statement.setString(32, entry.getGisaidEpiIsl());
                        statement.setDate(33, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setObject(34, entry.getYear());
                        statement.setObject(35, entry.getMonth());
                        statement.setObject(36, entry.getDay());
                        statement.setString(37, entry.getDateOriginal());
                        statement.setDate(38, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(39, entry.getRegion());
                        statement.setString(40, entry.getCountry());
                        statement.setString(41, entry.getDivision());
                        statement.setString(42, entry.getLocation());
                        statement.setString(43, entry.getRegionExposure());
                        statement.setString(44, entry.getCountryExposure());
                        statement.setString(45, entry.getDivisionExposure());
                        statement.setString(46, entry.getHost());
                        statement.setObject(47, entry.getAge());
                        statement.setString(48, entry.getSex());
                        statement.setString(49, entry.getSamplingStrategy());
                        statement.setString(50, entry.getPangoLineage());
                        statement.setString(51, entry.getNextcladePangoLineage());
                        statement.setString(52, entry.getNextstrainClade());
                        statement.setString(53, entry.getGisaidClade());
                        statement.setString(54, entry.getOriginatingLab());
                        statement.setString(55, entry.getSubmittingLab());
                        statement.setString(56, entry.getAuthors());
                        statement.setString(57, currentHash);

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
        log.info("finished updateMetadata");
    }


    private void updateNextcladeData() throws IOException, SQLException {
        log.info("started updateNextcladeData");

        InputStream fileInputStream = new FileInputStream(workdir.resolve("nextclade.tsv.gz").toFile());
        GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);

        String sql = """
                insert into y_nextstrain_genbank (
                  strain, aa_mutations, aa_insertions, nuc_substitutions, nuc_deletions, nuc_insertions,
                  nextclade_total_substitutions, nextclade_total_deletions, nextclade_total_insertions,
                  nextclade_total_frame_shifts, nextclade_total_aminoacid_substitutions,
                  nextclade_total_aminoacid_deletions, nextclade_total_aminoacid_insertions, nextclade_total_missing,
                  nextclade_total_non_acgtns, nextclade_total_pcr_primer_changes, nextclade_pcr_primer_changes,
                  nextclade_alignment_score, nextclade_alignment_start, nextclade_alignment_end,
                  nextclade_qc_overall_score, nextclade_qc_overall_status,
                  nextclade_qc_missing_data_missing_data_threshold, nextclade_qc_missing_data_score,
                  nextclade_qc_missing_data_status, nextclade_qc_missing_data_total_missing,
                  nextclade_qc_mixed_sites_mixed_sites_threshold, nextclade_qc_mixed_sites_score,
                  nextclade_qc_mixed_sites_status, nextclade_qc_mixed_sites_total_mixed_sites,
                  nextclade_qc_private_mutations_cutoff, nextclade_qc_private_mutations_excess,
                  nextclade_qc_private_mutations_score, nextclade_qc_private_mutations_status,
                  nextclade_qc_private_mutations_total, nextclade_qc_snp_clusters_clustered_snps,
                  nextclade_qc_snp_clusters_score, nextclade_qc_snp_clusters_status,
                  nextclade_qc_snp_clusters_total_snps, nextclade_qc_frame_shifts_frame_shifts,
                  nextclade_qc_frame_shifts_total_frame_shifts, nextclade_qc_frame_shifts_frame_shifts_ignored,
                  nextclade_qc_frame_shifts_total_frame_shifts_ignored, nextclade_qc_frame_shifts_score,
                  nextclade_qc_frame_shifts_status, nextclade_qc_stop_codons_stop_codons,
                  nextclade_qc_stop_codons_total_stop_codons, nextclade_qc_stop_codons_score,
                  nextclade_qc_stop_codons_status, nextclade_coverage, nextclade_errors
                )
                values (
                  ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?
                )
                on conflict (strain) do update
                set
                  aa_mutations = ?,
                  aa_insertions = ?,
                  nuc_substitutions = ?,
                  nuc_deletions = ?,
                  nuc_insertions = ?,
                  nextclade_total_substitutions = ?,
                  nextclade_total_deletions = ?,
                  nextclade_total_insertions = ?,
                  nextclade_total_frame_shifts = ?,
                  nextclade_total_aminoacid_substitutions = ?,
                  nextclade_total_aminoacid_deletions = ?,
                  nextclade_total_aminoacid_insertions = ?,
                  nextclade_total_missing = ?,
                  nextclade_total_non_acgtns = ?,
                  nextclade_total_pcr_primer_changes = ?,
                  nextclade_pcr_primer_changes = ?,
                  nextclade_alignment_score = ?,
                  nextclade_alignment_start = ?,
                  nextclade_alignment_end = ?,
                  nextclade_qc_overall_score = ?,
                  nextclade_qc_overall_status = ?,
                  nextclade_qc_missing_data_missing_data_threshold = ?,
                  nextclade_qc_missing_data_score = ?,
                  nextclade_qc_missing_data_status = ?,
                  nextclade_qc_missing_data_total_missing = ?,
                  nextclade_qc_mixed_sites_mixed_sites_threshold = ?,
                  nextclade_qc_mixed_sites_score = ?,
                  nextclade_qc_mixed_sites_status = ?,
                  nextclade_qc_mixed_sites_total_mixed_sites = ?,
                  nextclade_qc_private_mutations_cutoff = ?,
                  nextclade_qc_private_mutations_excess = ?,
                  nextclade_qc_private_mutations_score = ?,
                  nextclade_qc_private_mutations_status = ?,
                  nextclade_qc_private_mutations_total = ?,
                  nextclade_qc_snp_clusters_clustered_snps = ?,
                  nextclade_qc_snp_clusters_score = ?,
                  nextclade_qc_snp_clusters_status = ?,
                  nextclade_qc_snp_clusters_total_snps = ?,
                  nextclade_qc_frame_shifts_frame_shifts = ?,
                  nextclade_qc_frame_shifts_total_frame_shifts = ?,
                  nextclade_qc_frame_shifts_frame_shifts_ignored = ?,
                  nextclade_qc_frame_shifts_total_frame_shifts_ignored = ?,
                  nextclade_qc_frame_shifts_score = ?,
                  nextclade_qc_frame_shifts_status = ?,
                  nextclade_qc_stop_codons_stop_codons = ?,
                  nextclade_qc_stop_codons_total_stop_codons = ?,
                  nextclade_qc_stop_codons_score = ?,
                  nextclade_qc_stop_codons_status = ?,
                  nextclade_coverage = ?,
                  nextclade_errors = ?;
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
                        statement.setString(3, entry.getAaInsertions());
                        statement.setString(4, entry.getNucSubstitutions());
                        statement.setString(5, entry.getNucDeletions());
                        statement.setString(6, entry.getNucInsertions());
                        statement.setObject(7, entry.getTotalSubstitutions());
                        statement.setObject(8, entry.getTotalDeletions());
                        statement.setObject(9, entry.getTotalInsertions());
                        statement.setObject(10, entry.getTotalFrameShifts());
                        statement.setObject(11, entry.getTotalAminoacidSubstitutions());
                        statement.setObject(12, entry.getTotalAminoacidDeletions());
                        statement.setObject(13, entry.getTotalAminoacidInsertions());
                        statement.setObject(14, entry.getTotalMissing());
                        statement.setObject(15, entry.getTotalNonACGTNs());
                        statement.setObject(16, entry.getTotalPcrPrimerChanges());
                        statement.setString(17, entry.getPcrPrimerChanges());
                        statement.setObject(18, entry.getAlignmentScore());
                        statement.setObject(19, entry.getAlignmentStart());
                        statement.setObject(20, entry.getAlignmentEnd());
                        statement.setObject(21, entry.getQcOverallScore());
                        statement.setString(22, entry.getQcOverallStatus());
                        statement.setObject(23, entry.getQcMissingDataMissingDataThreshold());
                        statement.setObject(24, entry.getQcMissingDataScore());
                        statement.setString(25, entry.getQcMissingDataStatus());
                        statement.setObject(26, entry.getQcMissingDataTotalMissing());
                        statement.setObject(27, entry.getQcMixedSitesMixedSitesThreshold());
                        statement.setObject(28, entry.getQcMixedSitesScore());
                        statement.setString(29, entry.getQcMixedSitesStatus());
                        statement.setObject(30, entry.getQcMixedSitesTotalMixedSites());
                        statement.setObject(31, entry.getQcPrivateMutationsCutoff());
                        statement.setObject(32, entry.getQcPrivateMutationsExcess());
                        statement.setObject(33, entry.getQcPrivateMutationsScore());
                        statement.setString(34, entry.getQcPrivateMutationsStatus());
                        statement.setObject(35, entry.getQcPrivateMutationsTotal());
                        statement.setString(36, entry.getQcSnpClustersClusteredSNPs());
                        statement.setObject(37, entry.getQcSnpClustersScore());
                        statement.setString(38, entry.getQcSnpClustersStatus());
                        statement.setObject(39, entry.getQcSnpClustersTotalSNPs());
                        statement.setString(40, entry.getQcFrameShiftsFrameShifts());
                        statement.setObject(41, entry.getQcFrameShiftsTotalFrameShifts());
                        statement.setString(42, entry.getQcFrameShiftsFrameShiftsIgnored());
                        statement.setObject(43, entry.getQcFrameShiftsTotalFrameShiftsIgnored());
                        statement.setObject(44, entry.getQcFrameShiftsScore());
                        statement.setString(45, entry.getQcFrameShiftsStatus());
                        statement.setString(46, entry.getQcStopCodonsStopCodons());
                        statement.setObject(47, entry.getQcStopCodonsTotalStopCodons());
                        statement.setObject(48, entry.getQcStopCodonsScore());
                        statement.setString(49, entry.getQcStopCodonsStatus());
                        statement.setObject(50, entry.getCoverage());
                        statement.setString(51, entry.getErrors());

                        statement.setString(52, entry.getAaMutations());
                        statement.setString(53, entry.getAaInsertions());
                        statement.setString(54, entry.getNucSubstitutions());
                        statement.setString(55, entry.getNucDeletions());
                        statement.setString(56, entry.getNucInsertions());
                        statement.setObject(57, entry.getTotalSubstitutions());
                        statement.setObject(58, entry.getTotalDeletions());
                        statement.setObject(59, entry.getTotalInsertions());
                        statement.setObject(60, entry.getTotalFrameShifts());
                        statement.setObject(61, entry.getTotalAminoacidSubstitutions());
                        statement.setObject(62, entry.getTotalAminoacidDeletions());
                        statement.setObject(63, entry.getTotalAminoacidInsertions());
                        statement.setObject(64, entry.getTotalMissing());
                        statement.setObject(65, entry.getTotalNonACGTNs());
                        statement.setObject(66, entry.getTotalPcrPrimerChanges());
                        statement.setString(67, entry.getPcrPrimerChanges());
                        statement.setObject(68, entry.getAlignmentScore());
                        statement.setObject(69, entry.getAlignmentStart());
                        statement.setObject(70, entry.getAlignmentEnd());
                        statement.setObject(71, entry.getQcOverallScore());
                        statement.setString(72, entry.getQcOverallStatus());
                        statement.setObject(73, entry.getQcMissingDataMissingDataThreshold());
                        statement.setObject(74, entry.getQcMissingDataScore());
                        statement.setString(75, entry.getQcMissingDataStatus());
                        statement.setObject(76, entry.getQcMissingDataTotalMissing());
                        statement.setObject(77, entry.getQcMixedSitesMixedSitesThreshold());
                        statement.setObject(78, entry.getQcMixedSitesScore());
                        statement.setString(79, entry.getQcMixedSitesStatus());
                        statement.setObject(80, entry.getQcMixedSitesTotalMixedSites());
                        statement.setObject(81, entry.getQcPrivateMutationsCutoff());
                        statement.setObject(82, entry.getQcPrivateMutationsExcess());
                        statement.setObject(83, entry.getQcPrivateMutationsScore());
                        statement.setString(84, entry.getQcPrivateMutationsStatus());
                        statement.setObject(85, entry.getQcPrivateMutationsTotal());
                        statement.setString(86, entry.getQcSnpClustersClusteredSNPs());
                        statement.setObject(87, entry.getQcSnpClustersScore());
                        statement.setString(88, entry.getQcSnpClustersStatus());
                        statement.setObject(89, entry.getQcSnpClustersTotalSNPs());
                        statement.setString(90, entry.getQcFrameShiftsFrameShifts());
                        statement.setObject(91, entry.getQcFrameShiftsTotalFrameShifts());
                        statement.setString(92, entry.getQcFrameShiftsFrameShiftsIgnored());
                        statement.setObject(93, entry.getQcFrameShiftsTotalFrameShiftsIgnored());
                        statement.setObject(94, entry.getQcFrameShiftsScore());
                        statement.setString(95, entry.getQcFrameShiftsStatus());
                        statement.setString(96, entry.getQcStopCodonsStopCodons());
                        statement.setObject(97, entry.getQcStopCodonsTotalStopCodons());
                        statement.setObject(98, entry.getQcStopCodonsScore());
                        statement.setString(99, entry.getQcStopCodonsStatus());
                        statement.setObject(100, entry.getCoverage());
                        statement.setString(101, entry.getErrors());

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
        log.info("finished updateNextcladeData");
    }


    private void updateDataVersion(LocalDateTime startTime) throws SQLException {
        log.info("updating data version");
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
        log.info("set data version to " + epoch);
    }

}
