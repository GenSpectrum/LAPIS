package ch.ethz.lapis.transform;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.util.DeflateSeqCompressor;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.javatuples.Pair;


public class TransformService {

    private final ComboPooledDataSource databasePool;
    private final int maxNumberWorkers;
    private final SeqCompressor alignedSeqCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.REFERENCE);
    private final SeqCompressor nucMutationColumnarCompressor = new DeflateSeqCompressor(
        DeflateSeqCompressor.DICT.ATCGNDEL);
    private final SeqCompressor aaColumnarCompressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AACODONS);

    public TransformService(ComboPooledDataSource databasePool, int maxNumberWorkers) {
        this.databasePool = databasePool;
        this.maxNumberWorkers = maxNumberWorkers;
    }


    public void mergeAndTransform(LapisConfig.Source source) throws SQLException, InterruptedException {
        // Fill the tables
        //     y_main_metadata_staging
        //     y_main_sequence_staging
        //     y_main_aa_sequence_staging
        if (source == LapisConfig.Source.NG) {
            pullFromNextstrainGenbankTable();
        } else if (source == LapisConfig.Source.GISAID) {
            pullFromGisaidTable();
        }
        // Fill the table y_main_sequence_columnar_staging
        transformSeqsToColumnar();
        // Fill the table y_main_aa_sequence_columnar_staging
        transformAASeqsToColumnar();
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


    public void pullFromGisaidTable() throws SQLException {
        String sql1 = """
                insert into y_main_metadata_staging (
                  id, source, source_primary_key, genbank_accession, sra_accession, gisaid_epi_isl,
                  strain, date, date_submitted, region, country, division, location, region_exposure,
                  country_exposure, division_exposure, host, age, sex, sampling_strategy, pango_lineage,
                  nextstrain_clade, gisaid_clade, originating_lab, submitting_lab, authors
                )
                select
                  row_number() over () - 1 as id,
                  'gisaid' as source,
                  gisaid_epi_isl as source_primary_key,
                  null,
                  null,
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
                  null,
                  gisaid_clade,
                  originating_lab,
                  submitting_lab,
                  authors
                from y_gisaid
                where
                  seq_aligned_compressed is not null;
            """;
        String sql2 = """
                insert into y_main_sequence_staging (
                  id, seq_original_compressed, seq_aligned_compressed, aa_mutations, nuc_substitutions,
                  nuc_deletions, nuc_insertions
                )
                select
                  mm.id,
                  g.seq_original_compressed,
                  g.seq_aligned_compressed,
                  g.aa_mutations,
                  g.nuc_substitutions,
                  g.nuc_deletions,
                  g.nuc_insertions
                from
                  y_gisaid g
                  join y_main_metadata_staging mm
                    on mm.source = 'gisaid' and mm.source_primary_key = g.gisaid_epi_isl;
            """;
        String sql3 = """
                insert into y_main_aa_sequence_staging (id, gene, aa_seq)
                select
                  mm.id,
                  split_part(aa.gene_and_seq, ':', 1) as gene,
                  split_part(aa.gene_and_seq, ':', 2) as aa_seq
                from
                  y_gisaid g
                  join y_main_metadata_staging mm
                    on mm.source = 'gisaid' and mm.source_primary_key = g.gisaid_epi_isl,
                  unnest(string_to_array(g.aa_seqs, ',')) aa(gene_and_seq)
                where
                  g.aa_seqs is not null;
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
        // This function has two main constraints/bottlenecks:
        //   - Each column has as many characters as the number of sequences. As of 16.09.2021, GISAID is starting to
        //     get closer to 4 million sequences. 4mil x 1byte = 4 MB. Due to some copy&pasting, let's assume that
        //     processing a column will take 12 MB RAM. Processing 1000 bases at the same time then needs 12 GB RAM.
        //     Theoretically... We are using Java, so no clue how much RAM it is really going to take. But definitely
        //     much more!
        //     (The SARS-CoV-2 genomes has 29903 bases.)
        //   - Uncompressing millions of sequences needs a lot of CPU power.
        int columnsBatchSize = 3000; // This is the core value to balance the needed RAM, CPU and wall-clock time.
        int numberIterations = (int) Math.ceil(29903.0 / columnsBatchSize);
        int sequencesBatchSize = 20000;
        int numberTasksPerIteration = (int) Math.ceil(compressedSequences.size() * 1.0 / sequencesBatchSize);
        ExecutorService executor = Executors.newFixedThreadPool(maxNumberWorkers);
        for (int columnBatchIndex = 0; columnBatchIndex < numberIterations; columnBatchIndex++) {
            final int startPos = columnsBatchSize * columnBatchIndex;
            final int endPos = Math.min(columnsBatchSize * (columnBatchIndex + 1), 29903);
            System.out.println(LocalDateTime.now() + " Position " + startPos + " - " + endPos);

            List<Callable<List<StringBuilder>>> tasks = new ArrayList<>();
            for (int taskIndex = 0; taskIndex < numberTasksPerIteration; taskIndex++) {
                final int startSeq = sequencesBatchSize * taskIndex;
                final int endSeq = Math.min(sequencesBatchSize * (taskIndex + 1), compressedSequences.size());

                tasks.add(() -> {
                    System.out.println(
                        LocalDateTime.now() + "     Sequences " + startSeq + " - " + endSeq + " - Start");
                    List<StringBuilder> columns = new ArrayList<>();
                    for (int i = startPos; i < endPos; i++) {
                        columns.add(new StringBuilder());
                    }
                    for (int seqIndex = startSeq; seqIndex < endSeq; seqIndex++) {
                        byte[] compressed = compressedSequences.get(seqIndex).getValue1();
                        char[] seq = alignedSeqCompressor.decompress(compressed).toCharArray();
                        for (int i = startPos; i < endPos; i++) {
                            columns.get(i - startPos).append(seq[i]);
                        }
                    }
                    System.out.println(LocalDateTime.now() + "     Sequences " + startSeq + " - " + endSeq + " - End");
                    return columns;
                });
            }

            List<List<StringBuilder>> tasksResults = executor.invokeAll(tasks).stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

            // Concatenate the partial strings, compress them and insert
            // This will be done in parallel again.
            String sql2 = """
                    insert into y_main_sequence_columnar_staging (position, data_compressed)
                    values (?, ?);
                """;
            int finalizationBatchSize = 50;
            int countPos = endPos - startPos;
            int numberFinalizationTasks = (int) Math.ceil(countPos * 1.0 / finalizationBatchSize);
            List<Callable<Void>> tasks2 = new ArrayList<>();
            for (int finalizationIndex = 0; finalizationIndex < numberFinalizationTasks; finalizationIndex++) {
                final int finalizationPosStart = startPos + finalizationBatchSize * finalizationIndex;
                final int finalizationPosEnd = Math.min(startPos + finalizationBatchSize * (finalizationIndex + 1),
                    endPos);

                tasks2.add(() -> {
                    System.out.println(LocalDateTime.now() + "     Start compressing and inserting " +
                        finalizationPosStart + " - " + finalizationPosEnd);
                    for (int posIndex = finalizationPosStart; posIndex < finalizationPosEnd; posIndex++) {
                        StringBuilder fullColumn = new StringBuilder();
                        for (List<StringBuilder> tasksResult : tasksResults) {
                            fullColumn.append(tasksResult.get(posIndex - startPos));
                        }
                        byte[] compressed = nucMutationColumnarCompressor.compress(fullColumn.toString());
                        try (Connection conn = databasePool.getConnection()) {
                            try (PreparedStatement statement = conn.prepareStatement(sql2)) {
                                statement.setInt(1, posIndex + 1);
                                statement.setBytes(2, compressed);
                                statement.execute();
                            }
                        }
                    }
                    return null;
                });
            }
            List<Future<Void>> futures = executor.invokeAll(tasks2);
            try {
                for (Future<Void> future : futures) {
                    future.get();
                }
            } catch (ExecutionException e) {
                executor.shutdown();
                executor.awaitTermination(3, TimeUnit.MINUTES);
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.MINUTES);
    }


    public void transformAASeqsToColumnar() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(maxNumberWorkers, 4));
        List<Callable<Void>> tasks = new ArrayList<>();
        // We process the data gene by gene
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
            tasks.add(() -> {
                try (Connection conn = databasePool.getConnection()) {
                    System.out.println(LocalDateTime.now() + "Gene " + gene + " - Start processing");
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
                    System.out.println(LocalDateTime.now() + "Gene " + gene + " - Data loaded");
                    if (aaSequences.isEmpty()) {
                        System.out.println(LocalDateTime.now() + "Gene " + gene + " - No data found.");
                    }

                    // Read the sequences and create columnar data
                    int aaSeqLength = -1;
                    for (Pair<Integer, String> p : aaSequences) {
                        if (p.getValue1() != null) {
                            aaSeqLength = p.getValue1().length();
                            break;
                        }
                    }
                    int batchSize = 1500;
                    double iterations = Math.ceil(aaSeqLength * 1.0 / batchSize);
                    conn.setAutoCommit(false);
                    for (int j = 0; j < iterations; j++) {
                        int start = batchSize * j;
                        int end = Math.min(batchSize * (j + 1), aaSeqLength);
                        System.out.println(LocalDateTime.now() + "Gene " + gene + " - Position " + start + " - " + end);

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

                        List<String> columnStrs = columns.stream().map(StringBuilder::toString)
                            .collect(Collectors.toList());
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
                return null;
            });
        }
        List<Future<Void>> futures = executor.invokeAll(tasks);
        try {
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.MINUTES);
        }
    }


    public void mergeAdditionalMetadataFromS3c() throws SQLException {
        String sql = """
                update y_main_metadata_staging m
                set
                  age = a.age,
                  sex = a.sex,
                  hospitalized = a.hospitalized,
                  died = a.died,
                  fully_vaccinated = null -- TODO change this to "a.fully_vaccinated" once we decide to use it
                from y_s3c a
                where
                  m.gisaid_epi_isl = a.gisaid_epi_isl or m.sra_accession = a.sra_accession;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        }
    }


    /**
     * Switch the _staging tables with the active tables and update value in data_version
     */
    public void switchInStagingTables() throws SQLException {
        String sql1 = "select y_switch_in_staging_tables()";
        String sql2 = """
                insert into data_version (dataset, timestamp)
                values ('merged', extract(epoch from now())::bigint)
                on conflict (dataset) do update
                set
                  timestamp = extract(epoch from now())::bigint;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql1);
            }
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql2);
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }
}
