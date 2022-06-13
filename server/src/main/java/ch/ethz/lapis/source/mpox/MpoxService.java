package ch.ethz.lapis.source.mpox;

import ch.ethz.lapis.source.MutationFinder;
import ch.ethz.lapis.source.MutationNuc;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class MpoxService {

    private final ComboPooledDataSource databasePool;
    private final Path workdir;
    private final SeqCompressor seqCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.MPOX_REFERENCE);
    private int alignedSequenceLength = 0; // Will be set later.


    public MpoxService(
        ComboPooledDataSource databasePool,
        String workdir
    ) {
        this.databasePool = databasePool;
        this.workdir = Path.of(workdir);
    }


    public void updateData() throws IOException, SQLException {
        LocalDateTime startTime = LocalDateTime.now();

        // Delete existing data: no need to do any change detection for such a small dataset
        deleteAll();

        // The files and different types of data will be inserted/updated independently and one after the other in the
        // following order:
        //   1. original sequences
        //   2. aligned sequences
        //   3. metadata
        //   4. Nextclade data
        //   5. nucleotide mutations

        updateSeqOriginalOrAligned(false);
        updateSeqOriginalOrAligned(true);
        updateMetadata();
        updateNextcladeData();
//        fillSequencesWithoutAlignment();
        updateNucleotideMutations();

        updateDataVersion(startTime);
    }


    private void deleteAll() throws SQLException {
        String sql = """
                delete from y_nextstrain_mpox;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        }
    }


    private void updateSeqOriginalOrAligned(boolean aligned) throws IOException, SQLException {
        String filename = !aligned ? "sequences.fasta" : "nextclade-output/sequences.aligned.fasta";
        String sql = """
                insert into y_nextstrain_mpox (accession, seq_original_compressed, seq_original_hash)
                values (?, ?, ?)
                on conflict (accession) do update
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
                try (FastaFileReader fastaReader = new FastaFileReader(workdir.resolve(filename), false)) {
                    for (FastaEntry entry : fastaReader) {
                        String seq = aligned ? entry.getSeq().toUpperCase() : entry.getSeq();
                        if (aligned && alignedSequenceLength == 0) {
                            alignedSequenceLength = seq.length();
                        }
                        String sampleName = entry.getSampleName();
                        String currentHash = Utils.hashMd5(seq);
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
     * If a sequences could not be aligned, we will put NNNNNNN as a placeholder. This makes it possible to include all
     * sequences.
     */
    private void fillSequencesWithoutAlignment() throws SQLException {
        String placeholderSequence = "N".repeat(alignedSequenceLength);
        byte[] compressed = seqCompressor.compress(placeholderSequence);
        String hash = Utils.hashMd5(placeholderSequence);
        String sql = """
                update y_nextstrain_mpox
                set
                  seq_aligned_compressed = ?,
                  seq_aligned_hash = ?
                where
                  seq_aligned_compressed is null
                  and seq_original_compressed is not null;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setBytes(1, compressed);
                statement.setString(2, hash);
                statement.execute();
            }
        }
    }


    private void updateMetadata() throws IOException, SQLException {
        InputStream fileInputStream = new FileInputStream(workdir.resolve("metadata.tsv").toFile());

        String sql = """
                insert into y_nextstrain_mpox (
                  metadata_hash, accession, accession_rev, strain, sra_accession, date, year, month, day,
                  date_original, date_submitted, region, country, division, location, host, authors, institution
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (accession) do update
                set
                  metadata_hash = ?,
                  accession_rev = ?,
                  strain = ?,
                  sra_accession = ?,
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
                  host = ?,
                  authors = ?,
                  institution = ?;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (MpoxMetadataFileReader metadataReader = new MpoxMetadataFileReader(fileInputStream)) {
                    int i = 0;
                    for (MpoxMetadataEntry entry : metadataReader) {
                        if (entry.getStrain() == null) {
                            continue;
                        }
                        String currentHash = Utils.hashMd5(entry);

                        statement.setString(1, currentHash);
                        statement.setString(2, entry.getAccession());
                        statement.setString(3, entry.getAccessionRev());
                        statement.setString(4, entry.getStrain());
                        statement.setString(5, entry.getSraAccession());
                        statement.setDate(6, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setObject(7, entry.getYear());
                        statement.setObject(8, entry.getMonth());
                        statement.setObject(9, entry.getDay());
                        statement.setString(10, entry.getDateOriginal());
                        statement.setDate(11, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(12, entry.getRegion());
                        statement.setString(13, entry.getCountry());
                        statement.setString(14, entry.getDivision());
                        statement.setString(15, entry.getLocation());
                        statement.setString(16, entry.getHost());
                        statement.setString(17, entry.getAuthors());
                        statement.setString(18, entry.getInstitution());
                        // We use the clade from Nextclade

                        statement.setString(19, currentHash);
                        statement.setString(20, entry.getAccessionRev());
                        statement.setString(21, entry.getStrain());
                        statement.setString(22, entry.getSraAccession());
                        statement.setDate(23, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setObject(24, entry.getYear());
                        statement.setObject(25, entry.getMonth());
                        statement.setObject(26, entry.getDay());
                        statement.setString(27, entry.getDateOriginal());
                        statement.setDate(28, Utils.nullableSqlDateValue(entry.getDateSubmitted()));
                        statement.setString(29, entry.getRegion());
                        statement.setString(30, entry.getCountry());
                        statement.setString(31, entry.getDivision());
                        statement.setString(32, entry.getLocation());
                        statement.setString(33, entry.getHost());
                        statement.setString(34, entry.getAuthors());
                        statement.setString(35, entry.getInstitution());

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
        InputStream fileInputStream = new FileInputStream(workdir.resolve("nextclade-output/sequences.tsv").toFile());

        String sql = """
                insert into y_nextstrain_mpox (
                  accession, clade, nextclade_total_substitutions, nextclade_total_deletions,
                  nextclade_total_insertions, nextclade_total_frame_shifts, nextclade_total_aminoacid_substitutions,
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
                  nextclade_qc_stop_codons_status, nextclade_errors
                )
                values (
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                on conflict (accession) do update
                set
                  clade = ?,
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
                  nextclade_errors = ?;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (NextcladeTsvFileReader nextcladeTsvFileReader = new NextcladeTsvFileReader(fileInputStream)) {
                    int i = 0;
                    for (NextcladeTsvEntry nc : nextcladeTsvFileReader) {
                        statement.setString(1, nc.getSeqName());
                        statement.setString(2, nc.getClade());
                        statement.setObject(3, nc.getTotalSubstitutions());
                        statement.setObject(4, nc.getTotalDeletions());
                        statement.setObject(5, nc.getTotalInsertions());
                        statement.setObject(6, nc.getTotalFrameShifts());
                        statement.setObject(7, nc.getTotalAminoacidSubstitutions());
                        statement.setObject(8, nc.getTotalAminoacidDeletions());
                        statement.setObject(9, nc.getTotalAminoacidInsertions());
                        statement.setObject(10, nc.getTotalMissing());
                        statement.setObject(11, nc.getTotalNonACGTNs());
                        statement.setObject(12, nc.getTotalPcrPrimerChanges());
                        statement.setObject(13, nc.getPcrPrimerChanges());
                        statement.setObject(14, nc.getAlignmentScore());
                        statement.setObject(15, nc.getAlignmentStart());
                        statement.setObject(16, nc.getAlignmentEnd());
                        statement.setObject(17, nc.getQcOverallScore());
                        statement.setString(18, nc.getQcOverallStatus());
                        statement.setObject(19, nc.getQcMissingDataMissingDataThreshold());
                        statement.setObject(20, nc.getQcMissingDataScore());
                        statement.setString(21, nc.getQcMissingDataStatus());
                        statement.setObject(22, nc.getQcMissingDataTotalMissing());
                        statement.setObject(23, nc.getQcMixedSitesMixedSitesThreshold());
                        statement.setObject(24, nc.getQcMixedSitesScore());
                        statement.setString(25, nc.getQcMixedSitesStatus());
                        statement.setObject(26, nc.getQcMixedSitesTotalMixedSites());
                        statement.setObject(27, nc.getQcPrivateMutationsCutoff());
                        statement.setObject(28, nc.getQcPrivateMutationsExcess());
                        statement.setObject(29, nc.getQcPrivateMutationsScore());
                        statement.setString(30, nc.getQcPrivateMutationsStatus());
                        statement.setObject(31, nc.getQcPrivateMutationsTotal());
                        statement.setString(32, nc.getQcSnpClustersClusteredSNPs());
                        statement.setObject(33, nc.getQcSnpClustersScore());
                        statement.setString(34, nc.getQcSnpClustersStatus());
                        statement.setObject(35, nc.getQcSnpClustersTotalSNPs());
                        statement.setString(36, nc.getQcFrameShiftsFrameShifts());
                        statement.setObject(37, nc.getQcFrameShiftsTotalFrameShifts());
                        statement.setString(38, nc.getQcFrameShiftsFrameShiftsIgnored());
                        statement.setObject(39, nc.getQcFrameShiftsTotalFrameShiftsIgnored());
                        statement.setObject(40, nc.getQcFrameShiftsScore());
                        statement.setString(41, nc.getQcFrameShiftsStatus());
                        statement.setString(42, nc.getQcStopCodonsStopCodons());
                        statement.setObject(43, nc.getQcStopCodonsTotalStopCodons());
                        statement.setObject(44, nc.getQcStopCodonsScore());
                        statement.setString(45, nc.getQcStopCodonsStatus());
                        statement.setString(46, nc.getErrors());

                        statement.setString(47, nc.getClade());
                        statement.setObject(48, nc.getTotalSubstitutions());
                        statement.setObject(49, nc.getTotalDeletions());
                        statement.setObject(50, nc.getTotalInsertions());
                        statement.setObject(51, nc.getTotalFrameShifts());
                        statement.setObject(52, nc.getTotalAminoacidSubstitutions());
                        statement.setObject(53, nc.getTotalAminoacidDeletions());
                        statement.setObject(54, nc.getTotalAminoacidInsertions());
                        statement.setObject(55, nc.getTotalMissing());
                        statement.setObject(56, nc.getTotalNonACGTNs());
                        statement.setObject(57, nc.getTotalPcrPrimerChanges());
                        statement.setObject(58, nc.getPcrPrimerChanges());
                        statement.setObject(59, nc.getAlignmentScore());
                        statement.setObject(60, nc.getAlignmentStart());
                        statement.setObject(61, nc.getAlignmentEnd());
                        statement.setObject(62, nc.getQcOverallScore());
                        statement.setString(63, nc.getQcOverallStatus());
                        statement.setObject(64, nc.getQcMissingDataMissingDataThreshold());
                        statement.setObject(65, nc.getQcMissingDataScore());
                        statement.setString(66, nc.getQcMissingDataStatus());
                        statement.setObject(67, nc.getQcMissingDataTotalMissing());
                        statement.setObject(68, nc.getQcMixedSitesMixedSitesThreshold());
                        statement.setObject(69, nc.getQcMixedSitesScore());
                        statement.setString(70, nc.getQcMixedSitesStatus());
                        statement.setObject(71, nc.getQcMixedSitesTotalMixedSites());
                        statement.setObject(72, nc.getQcPrivateMutationsCutoff());
                        statement.setObject(73, nc.getQcPrivateMutationsExcess());
                        statement.setObject(74, nc.getQcPrivateMutationsScore());
                        statement.setString(75, nc.getQcPrivateMutationsStatus());
                        statement.setObject(76, nc.getQcPrivateMutationsTotal());
                        statement.setString(77, nc.getQcSnpClustersClusteredSNPs());
                        statement.setObject(78, nc.getQcSnpClustersScore());
                        statement.setString(79, nc.getQcSnpClustersStatus());
                        statement.setObject(80, nc.getQcSnpClustersTotalSNPs());
                        statement.setString(81, nc.getQcFrameShiftsFrameShifts());
                        statement.setObject(82, nc.getQcFrameShiftsTotalFrameShifts());
                        statement.setString(83, nc.getQcFrameShiftsFrameShiftsIgnored());
                        statement.setObject(84, nc.getQcFrameShiftsTotalFrameShiftsIgnored());
                        statement.setObject(85, nc.getQcFrameShiftsScore());
                        statement.setString(86, nc.getQcFrameShiftsStatus());
                        statement.setString(87, nc.getQcStopCodonsStopCodons());
                        statement.setObject(88, nc.getQcStopCodonsTotalStopCodons());
                        statement.setObject(89, nc.getQcStopCodonsScore());
                        statement.setString(90, nc.getQcStopCodonsStatus());
                        statement.setString(91, nc.getErrors());

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


    private void updateNucleotideMutations() throws IOException, SQLException {
        String filename = "nextclade-output/sequences.aligned.fasta";
        String sql = """
                insert into y_nextstrain_mpox (accession, nuc_substitutions, nuc_deletions, nuc_unknowns)
                values (?, ?, ?, ?)
                on conflict (accession) do update
                set
                  nuc_substitutions = ?,
                  nuc_deletions = ?,
                  nuc_unknowns = ?;
            """;
        int i = 0;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (FastaFileReader fastaReader = new FastaFileReader(workdir.resolve(filename), false)) {
                    ReferenceGenomeData refGenome = ReferenceGenomeData.getInstance();
                    for (FastaEntry entry : fastaReader) {
                        String seq = entry.getSeq().toUpperCase();
                        List<MutationNuc> nucMutations = MutationFinder.findNucMutations(seq);
                        String nucSubstitutions = nucMutations.stream()
                            .filter(m -> !m.getMutation().equals("-"))
                            .map(m -> String.valueOf(refGenome.getNucleotideBase(m.getPosition())) + m.getPosition()
                                + m.getMutation())
                            .collect(Collectors.joining(","));
                        String nucDeletions = nucMutations.stream()
                            .filter(m -> m.getMutation().equals("-"))
                            .map(m -> String.valueOf(refGenome.getNucleotideBase(m.getPosition())) + m.getPosition()
                                + m.getMutation())
                            .collect(Collectors.joining(","));
                        String nucUnknowns = String.join(",", MutationFinder.compressPositionsAsStrings(
                            MutationFinder.findNucUnknowns(seq)));

                        String sampleName = entry.getSampleName();
                        statement.setString(1, sampleName);
                        statement.setString(2, nucSubstitutions);
                        statement.setString(3, nucDeletions);
                        statement.setString(4, nucUnknowns);

                        statement.setString(5, nucSubstitutions);
                        statement.setString(6, nucDeletions);
                        statement.setString(7, nucUnknowns);
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


    private void updateDataVersion(LocalDateTime startTime) throws SQLException {
        ZoneId zoneId = ZoneId.systemDefault();
        long epoch = startTime.atZone(zoneId).toEpochSecond();
        String sql = """
                insert into data_version (dataset, timestamp)
                values ('mpox', ?)
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
