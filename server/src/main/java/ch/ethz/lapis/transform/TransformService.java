package ch.ethz.lapis.transform;

import ch.ethz.lapis.util.DeflateSeqCompressor;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.javatuples.Pair;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class TransformService {

    private final ComboPooledDataSource databasePool;
    private final int maxNumberWorkers;
    private final SeqCompressor alignedSeqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);
    private final SeqCompressor nucMutationColumnarCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL);
    private final SeqCompressor aaColumnarCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AACODONS);

    public TransformService(ComboPooledDataSource databasePool, int maxNumberWorkers) {
        this.databasePool = databasePool;
        this.maxNumberWorkers = maxNumberWorkers;
    }


    public void mergeAndTransform() throws SQLException, InterruptedException {
        // Fill the tables
        //     y_main_metadata_staging
        //     y_main_sequence_staging
        //     y_main_aa_sequence_staging
        pullFromNextstrainGenbankTable();
        // Fill the table y_main_sequence_columnar_staging
        transformSeqsToColumnar();
        // Fill the table y_main_aa_sequence_columnar_staging
        transformAASeqsToColumnar();
        // Switch the _staging tables with the active tables
        switchInStagingTables();
    }


    public void pullFromNextstrainGenbankTable() throws SQLException {
        String sql1 = """
                insert into y_main_metadata_staging (
                  id, source, source_primary_key, genbank_accession, sra_accession, gisaid_epi_isl,
                  strain, date, date_submitted, region, country, division, location, region_exposure,
                  country_exposure, division_exposure, host, age, sex, sampling_strategy, pango_lineage,
                  nextstrain_clade, gisaid_clade, originating_lab, submitting_lab, authors
                )
                select
                  row_number() over () - 1 as id,
                  'nextstrain/genbank' as source,
                  strain as source_primary_key,
                  genbank_accession,
                  sra_accession,
                  gisaid_epi_isl,
                  strain,
                  date,
                  date_submitted,
                  region,
                  country,
                  division,
                  location,
                  region_exposure,
                  country_exposure,
                  division_exposure,
                  host,
                  age,
                  sex,
                  sampling_strategy,
                  pango_lineage,
                  nextstrain_clade,
                  gisaid_clade,
                  originating_lab,
                  submitting_lab,
                  authors
                from y_nextstrain_genbank
                where
                  genbank_accession is not null
                  and seq_aligned_compressed is not null;
        """;
        String sql2 = """
            insert into y_main_sequence_staging (
              id, seq_original_compressed, seq_aligned_compressed, aa_mutations, nuc_substitutions,
              nuc_deletions, nuc_insertions
            )
            select
              mm.id,
              ng.seq_original_compressed,
              ng.seq_aligned_compressed,
              ng.aa_mutations,
              ng.nuc_substitutions,
              ng.nuc_deletions,
              ng.nuc_insertions
            from
              y_nextstrain_genbank ng
              join y_main_metadata_staging mm
                on mm.source = 'nextstrain/genbank' and mm.source_primary_key = ng.strain;
        """;
        String sql3 = """
            insert into y_main_aa_sequence_staging (id, gene, aa_seq)
            select
              mm.id,
              split_part(aa.gene_and_seq, ':', 1) as gene,
              split_part(aa.gene_and_seq, ':', 2) as aa_seq
            from
              y_nextstrain_genbank ng
              join y_main_metadata_staging mm
                on mm.source = 'nextstrain/genbank' and mm.source_primary_key = ng.strain,
              unnest(string_to_array(ng.aa_seqs, ',')) aa(gene_and_seq)
            where
              ng.aa_seqs is not null;
        """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql1);
                statement.execute(sql2);
                statement.execute(sql3);
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }


    public void transformSeqsToColumnar() throws SQLException, InterruptedException {
        // Load all compressed and aligned sequences and their IDs
        String sql1 = """
            select s.id, s.seq_aligned_compressed
            from y_main_sequence_staging s
            order by s.id;
        """;
        List<Pair<Integer, byte[]>> compressedSequences = new ArrayList<>();
        int idCounter = 0;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql1)) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        byte[] compressedSeq = rs.getBytes("seq_aligned_compressed");
                        if (id != idCounter) {
                            throw new RuntimeException("Weird.. I expected ID=" + idCounter + " but got " + id);
                        }
                        compressedSequences.add(new Pair<>(id, compressedSeq));
                        idCounter++;
                    }
                }
            }
        }
        System.out.println(compressedSequences.size());
        System.out.println("Data loaded");

        // Read the sequences and create columnar data
        ExecutorService executor = Executors.newFixedThreadPool(maxNumberWorkers);
        for (int j = 0; j < 15; j++) {
            final int start = 2000 * j;
            final int end = Math.min(2000 * (j + 1), 29903);
            executor.execute(() -> {
                try (Connection conn = databasePool.getConnection()) {
                    System.out.println(LocalDateTime.now() +  " Position " + start + " - " +end);

                    List<StringBuilder> columns = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        columns.add(new StringBuilder());
                    }

                    for (Pair<Integer, byte[]> compressedSequence : compressedSequences) {
                        byte[] compressed = compressedSequence.getValue1();
                        char[] seq = alignedSeqCompressor.decompress(compressed).toCharArray();
                        for (int i = start; i < end; i++) {
                            columns.get(i - start).append(seq[i]);
                        }
                    }

                    List<String> columnStrs = columns.stream().map(StringBuilder::toString).collect(Collectors.toList());
                    List<byte[]> compressed = columnStrs.stream()
                            .map(nucMutationColumnarCompressor::compress)
                            .collect(Collectors.toList());

                    String sql2 = """
                    insert into y_main_sequence_columnar_staging (position, data_compressed)
                    values (?, ?);
                """;
                    try (PreparedStatement statement = conn.prepareStatement(sql2)) {
                        for (int i = start; i < end; i++) {
                            statement.setInt(1, i + 1);
                            statement.setBytes(2, compressed.get(i - start));
                            statement.addBatch();
                        }
                        statement.executeBatch();
                        statement.clearBatch();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }


    public void transformAASeqsToColumnar() throws SQLException {
        try (Connection conn = databasePool.getConnection()) {
            // We process one gene after another
            String sql1 = """
                select mm.id, aas.aa_seq
                from
                  y_main_metadata_staging mm
                  left join (
                    select *
                    from y_main_aa_sequence_staging
                    where gene = ?
                  ) aas on mm.id = aas.id
                order by mm.id;
            """;
            for (String gene : ReferenceGenomeData.getInstance().getGeneNames()) {
                System.out.println("Start processing gene " + gene);
                // Load all amino acid sequences and their IDs
                List<Pair<Integer, String>> aaSequences = new ArrayList<>();
                int idCounter = 0;
                try (PreparedStatement statement = conn.prepareStatement(sql1)) {
                    statement.setString(1, gene);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String seq = rs.getString("aa_seq");
                            if (id != idCounter) {
                                throw new RuntimeException("Weird.. I expected ID=" + idCounter + " but got " + id);
                            }
                            aaSequences.add(new Pair<>(id, seq));
                            idCounter++;
                        }
                    }
                }
                System.out.println("Data loaded");
                if (aaSequences.isEmpty()) {
                    System.out.println("No data found.");
                }

                // Read the sequences and create columnar data
                int aaSeqLength = -1;
                for (Pair<Integer, String> p : aaSequences) {
                    if (p.getValue1() != null) {
                        aaSeqLength = p.getValue1().length();
                        break;
                    }
                }
                double iterations = Math.ceil(aaSeqLength * 1.0 / 2000);
                conn.setAutoCommit(false);
                for (int j = 0; j < iterations; j++) {
                    int start = 2000 * j;
                    int end = Math.min(2000 * (j + 1), aaSeqLength);
                    System.out.println(LocalDateTime.now() +  " Position " + start + " - " +end);

                    List<StringBuilder> columns = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        columns.add(new StringBuilder());
                    }

                    for (Pair<Integer, String> p : aaSequences) {
                        char[] seq = p.getValue1() != null ? p.getValue1().toCharArray() : null;
                        char base;
                        for (int i = start; i < end; i++) {
                            if (seq != null) {
                                base = seq[i];
                            } else {
                                base = 'X'; // X = unknown
                            }
                            columns.get(i - start).append(base);
                        }
                    }

                    List<String> columnStrs = columns.stream().map(StringBuilder::toString).collect(Collectors.toList());
                    List<byte[]> compressed = columnStrs.stream()
                            .map(aaColumnarCompressor::compress)
                            .collect(Collectors.toList());

                    String sql2 = """
                        insert into y_main_aa_sequence_columnar_staging (position, gene, data_compressed)
                        values (?, ?, ?);
                    """;
                    try (PreparedStatement statement = conn.prepareStatement(sql2)) {
                        for (int i = start; i < end; i++) {
                            statement.setInt(1, i + 1);
                            statement.setString(2, gene);
                            statement.setBytes(3, compressed.get(i - start));
                            statement.addBatch();
                        }
                        Utils.executeClearCommitBatch(conn, statement);
                    }
                }
                conn.setAutoCommit(true);
            }
        }
    }


    public void switchInStagingTables() throws SQLException {
        String sql = "select y_switch_in_staging_tables()";
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        }
    }
}
