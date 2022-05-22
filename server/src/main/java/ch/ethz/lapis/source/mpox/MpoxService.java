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
        //   4. nucleotide mutations

        updateSeqOriginalOrAligned(false);
        updateSeqOriginalOrAligned(true);
        updateMetadata();
        fillSequencesWithoutAlignment();
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
        String filename = !aligned ? "sequences.fasta" : "aligned.fasta";
        String sql = """
                insert into y_nextstrain_mpox (strain, seq_original_compressed, seq_original_hash)
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
                  metadata_hash, strain, sra_accession, date, date_original,
                  region, country, host, clade
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (strain) do update
                set
                  metadata_hash = ?,
                  sra_accession = ?,
                  date = ?,
                  date_original = ?,
                  region = ?,
                  country = ?,
                  host = ?,
                  clade = ?;
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
                        statement.setString(2, entry.getStrain());
                        statement.setString(3, entry.getSraAccession());
                        statement.setDate(4, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setString(5, entry.getDateOriginal());
                        statement.setString(6, entry.getRegion());
                        statement.setString(7, entry.getCountry());
                        statement.setString(8, entry.getHost());
                        statement.setString(9, entry.getClade());

                        statement.setString(10, currentHash);
                        statement.setString(11, entry.getSraAccession());
                        statement.setDate(12, Utils.nullableSqlDateValue(entry.getDate()));
                        statement.setString(13, entry.getDateOriginal());
                        statement.setString(14, entry.getRegion());
                        statement.setString(15, entry.getCountry());
                        statement.setString(16, entry.getHost());
                        statement.setString(17, entry.getClade());

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
        String filename = "aligned.fasta";
        String sql = """
                insert into y_nextstrain_mpox (strain, nuc_substitutions, nuc_deletions, nuc_unknowns)
                values (?, ?, ?, ?)
                on conflict (strain) do update
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
