package ch.ethz.lapis.transform;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.core.DatabaseReaderQueueBuilder;
import ch.ethz.lapis.core.ExhaustibleBlockingQueue;
import ch.ethz.lapis.core.ExhaustibleLinkedBlockingQueue;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class TransformService {

    private final ComboPooledDataSource databasePool;
    private final int maxNumberWorkers;
    private final SeqCompressor alignedSeqCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE);
    private final SeqCompressor aaSeqCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.AA_REFERENCE);
    private static final SeqCompressor columnarCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);

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
            System.out.println(LocalDateTime.now() + " pullFromNextstrainGenbankTable()");
            pullFromNextstrainGenbankTable();
            // Compress AA sequences
            System.out.println(LocalDateTime.now() + " compressAASeqs()");
            compressAASeqs("""
                select
                  mm.id,
                  ng.aa_seqs_compressed
                from
                    y_nextstrain_genbank ng
                    join y_main_metadata_staging mm
                      on mm.source = 'nextstrain/genbank' and mm.source_primary_key = ng.strain
                where
                  ng.aa_seqs_compressed is not null;
            """);
        } else if (source == LapisConfig.Source.GISAID) {
            System.out.println(LocalDateTime.now() + " pullFromGisaidTable()");
            pullFromGisaidTable();
            // Compress AA sequences
            System.out.println(LocalDateTime.now() + " compressAASeqs()");
            compressAASeqs("""
                select
                  mm.id,
                  g.aa_seqs_compressed
                from
                  y_gisaid g
                  join y_main_metadata_staging mm
                    on mm.source = 'gisaid' and mm.source_primary_key = g.gisaid_epi_isl
                where
                  g.aa_seqs_compressed is not null;
            """);
        }
        // Fill the table y_main_sequence_columnar_staging
        System.out.println(LocalDateTime.now() + " transformSeqsToColumnar()");
        transformSeqsToColumnar();
        // Fill the table y_main_aa_sequence_columnar_staging
        System.out.println(LocalDateTime.now() + " transformAASeqsToColumnar()");
        transformAASeqsToColumnar();
    }


    public void pullFromNextstrainGenbankTable() throws SQLException {
        String sql1 = """
                    insert into y_main_metadata_staging (
                      id, source, source_primary_key, genbank_accession, sra_accession, gisaid_epi_isl,
                      strain, date, year, month, day, date_submitted, region, country, division, location, region_exposure,
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
                      year,
                      month,
                      day
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
                  id, seq_original_compressed, seq_aligned_compressed, aa_mutations, aa_unknowns, nuc_substitutions,
                  nuc_deletions, nuc_insertions, nuc_unknowns
                )
                select
                  mm.id,
                  ng.seq_original_compressed,
                  ng.seq_aligned_compressed,
                  ng.aa_mutations,
                  ng.aa_unknowns,
                  ng.nuc_substitutions,
                  ng.nuc_deletions,
                  ng.nuc_insertions,
                  ng.nuc_unknowns
                from
                  y_nextstrain_genbank ng
                  join y_main_metadata_staging mm
                    on mm.source = 'nextstrain/genbank' and mm.source_primary_key = ng.strain;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql1);
                statement.execute(sql2);
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }


    public void pullFromGisaidTable() throws SQLException {
        String sql1 = """
                insert into y_main_metadata_staging (
                  id, source, source_primary_key, genbank_accession, sra_accession, gisaid_epi_isl,
                  strain, date, year, month,day, date_submitted, region, country, division, location, region_exposure,
                  country_exposure, division_exposure, host, age, sex, sampling_strategy, pango_lineage,
                  nextclade_pango_lineage, nextstrain_clade, gisaid_clade, originating_lab, submitting_lab, authors,
                  nextclade_qc_overall_score, nextclade_qc_missing_data_score, nextclade_qc_mixed_sites_score,
                  nextclade_qc_private_mutations_score, nextclade_qc_snp_clusters_score,
                  nextclade_qc_frame_shifts_score, nextclade_qc_stop_codons_score
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
                  year,
                  monhth,
                  day,
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
                  nextclade_pango_lineage,
                  nextclade_clade,
                  gisaid_clade,
                  originating_lab,
                  submitting_lab,
                  authors,
                  nextclade_qc_overall_score,
                  nextclade_qc_missing_data_score,
                  nextclade_qc_mixed_sites_score,
                  nextclade_qc_private_mutations_score,
                  nextclade_qc_snp_clusters_score,
                  nextclade_qc_frame_shifts_score,
                  nextclade_qc_stop_codons_score
                from y_gisaid
                where
                  seq_aligned_compressed is not null;
            """;
        String sql2 = """
                insert into y_main_sequence_staging (
                  id, seq_original_compressed, seq_aligned_compressed, aa_mutations, aa_unknowns, nuc_substitutions,
                  nuc_deletions, nuc_insertions, nuc_unknowns
                )
                select
                  mm.id,
                  g.seq_original_compressed,
                  g.seq_aligned_compressed,
                  g.aa_mutations,
                  g.aa_unknowns,
                  g.nuc_substitutions,
                  g.nuc_deletions,
                  g.nuc_insertions,
                  g.nuc_unknowns
                from
                  y_gisaid g
                  join y_main_metadata_staging mm
                    on mm.source = 'gisaid' and mm.source_primary_key = g.gisaid_epi_isl;
            """;
        try (Connection conn = databasePool.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql1);
                statement.execute(sql2);
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }


    /**
     *
     * @param fetchAASeqsSql A SQL query string that gives a result set with two columns: "id" and "aa_seqs_compressed"
     */
    public void compressAASeqs(String fetchAASeqsSql) throws SQLException, InterruptedException {
        // Fetch the uncompressed AA sequences
        // The AA sequences have the format "<gene1>:<seq1>,<gene2>:<seq2>,..."
        ExhaustibleBlockingQueue<List<Pair<Integer, byte[]>>> batches;
        Connection conn = databasePool.getConnection();
        PreparedStatement statement = conn.prepareStatement(fetchAASeqsSql);
        var elementQueue = new DatabaseReaderQueueBuilder<>(
            statement,
            (rs) -> {
                try {
                    return new Pair<>(rs.getInt("id"), rs.getBytes("aa_seqs_compressed"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            50000, 20000
        ).build();
        batches = ExhaustibleLinkedBlockingQueue.batchElements(elementQueue, 500, 30);

        // Compress and write to database
        ExecutorService executor = Executors.newFixedThreadPool(maxNumberWorkers);
        AtomicBoolean failed = new AtomicBoolean(false);
        for (int i = 0; i < maxNumberWorkers; i++) {
            executor.submit(() -> {
                try {
                    while (!batches.isEmpty() || !batches.isExhausted()) {
                        // Get a batch from the queue
                        List<Pair<Integer, byte[]>> batch = batches.poll(3, TimeUnit.SECONDS);
                        if (batch == null) {
                            continue;
                        }
                        // Compress
                        // We will use a Triplet to store the elements.
                        // Triplet<Integer, String, byte[]> = (id, gene, aa_seq_compressed)
                        List<Triplet<Integer, String, byte[]>> compressed = new ArrayList<>();
                        for (Pair<Integer, byte[]> aaSeqsEntry : batch) {
                            String aaSeqs = aaSeqCompressor.decompress(aaSeqsEntry.getValue1());
                            if (aaSeqs == null || aaSeqs.isBlank()) {
                                continue;
                            }
                            for (String s : aaSeqs.split(",")) {
                                String[] tmp = s.split(":");
                                String gene = tmp[0];
                                String aaSeq = tmp[1];
                                compressed.add(new Triplet<>(
                                    aaSeqsEntry.getValue0(),
                                    gene,
                                    aaSeqCompressor.compress(aaSeq)
                                ));
                            }
                        }
                        // Write to database
                        String insertSql = """
                            insert into y_main_aa_sequence_staging (id, gene, aa_seq_compressed)
                            values (?, ?, ?);
                            """;
                        try (Connection conn2 = databasePool.getConnection()) {
                            conn2.setAutoCommit(false);
                            try (PreparedStatement statement2 = conn2.prepareStatement(insertSql)) {
                                for (Triplet<Integer, String, byte[]> entry : compressed) {
                                    statement2.setInt(1, entry.getValue0());
                                    statement2.setString(2, entry.getValue1());
                                    statement2.setBytes(3, entry.getValue2());
                                    statement2.addBatch();
                                }
                                Utils.executeClearCommitBatch(conn2, statement2);
                            }
                            conn2.setAutoCommit(true);
                        }
                    }
                } catch (InterruptedException | SQLException e) {
                    failed.set(true);
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.DAYS);
        if (failed.get()) {
            throw new RuntimeException("Execution failed.");
        }
    }


    public void transformSeqsToColumnar() throws SQLException {
        // Load all compressed and aligned sequences and their IDs
        String sql1 = """
                select s.id, s.seq_aligned_compressed
                from y_main_sequence_staging s
                order by s.id;
            """;
        List<byte[]> compressedSequences = new ArrayList<>();
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
                        compressedSequences.add(compressedSeq);
                        idCounter++;
                    }
                }
            }
        }
        System.out.println(LocalDateTime.now() + " Data loaded");

        SequenceRowToColumnTransformer transformer = new SequenceRowToColumnTransformer(maxNumberWorkers, 1500);
        transformer.transform(
            compressedSequences,
            alignedSeqCompressor::decompress,
            (pos, result) -> {
                String sql2 = """
                    insert into y_main_sequence_columnar_staging (position, data_compressed)
                    values (?, ?);
                """;
                try (Connection conn = databasePool.getConnection()) {
                    try (PreparedStatement statement = conn.prepareStatement(sql2)) {
                        for (int i = 0; i < result.size(); i++) {
                            byte[] compressed = result.get(i);
                            statement.setInt(1, pos + i);
                            statement.setBytes(2, compressed);
                            statement.execute();
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            columnarCompressor::compress,
            'N'
        );
    }


    public void transformAASeqsToColumnar() throws SQLException {
        // We process the data gene by gene
        String sql1 = """
            select mm.id, aas.aa_seq_compressed
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
            System.out.println(LocalDateTime.now() + "Gene " + gene + " - Start processing");
            // Load all amino acid sequences and their IDs
            List<byte[]> aaSequences = new ArrayList<>();
            int idCounter = 0;
            try (Connection conn = databasePool.getConnection()) {
                try (PreparedStatement statement = conn.prepareStatement(sql1)) {
                    statement.setString(1, gene);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            byte[] seq = rs.getBytes("aa_seq_compressed");
                            if (id != idCounter) {
                                throw new RuntimeException("Weird.. I expected ID=" + idCounter + " but got " + id);
                            }
                            aaSequences.add(seq);
                            idCounter++;
                        }
                    }
                }
            }
            System.out.println(LocalDateTime.now() + "Gene " + gene + " - Data loaded");
            if (aaSequences.isEmpty()) {
                System.out.println(LocalDateTime.now() + "Gene " + gene + " - No data found.");
            }

            SequenceRowToColumnTransformer transformer = new SequenceRowToColumnTransformer(maxNumberWorkers, 1500);
            transformer.transform(
                aaSequences,
                aaSeqCompressor::decompress,
                (pos, result) -> {
                    String sql2 = """
                        insert into y_main_aa_sequence_columnar_staging (position, gene, data_compressed)
                        values (?, ?, ?);
                    """;
                    try (Connection conn = databasePool.getConnection()) {
                        try (PreparedStatement statement = conn.prepareStatement(sql2)) {
                            for (int i = 0; i < result.size(); i++) {
                                byte[] compressed = result.get(i);
                                statement.setInt(1, pos + i);
                                statement.setString(2, gene);
                                statement.setBytes(3, compressed);
                                statement.execute();
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                },
                columnarCompressor::compress,
                'X'
            );
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


    public void mergeFromPangolinAssignment() throws SQLException {
        String sql = """
            update y_main_metadata_staging m
            set
              pango_lineage = a.pango_lineage
            from y_pangolin_assignment a
            where
              m.gisaid_epi_isl = a.gisaid_epi_isl;
        """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(sql);
            }
        }
    }


    public void finalTransforms() throws SQLException {

        try (Connection conn = databasePool.getConnection()) {

            try (Statement statement = conn.createStatement()) {
                // Change pangoLineage "None" and "" to null
                String sql1 = """
                    update y_main_metadata_staging m
                    set pango_lineage = null
                    where
                      m.pango_lineage = 'None'
                      or m.pango_lineage = ''
                      or m.pango_lineage = 'unclassifiable';
                """;
                statement.execute(sql1);
                // Clean up the sampling strategy field a bit
                String sql2 = """
                    update y_main_metadata_staging
                    set sampling_strategy = 'Baseline surveillance'
                    where
                      sampling_strategy in (
                        'Random sampling',
                        'Baseline surveillance (random sampling)',
                        'Baseline Surveillance',
                        'Surveillance testing',
                        'baseline surveillance (random sampling)',
                        'Active surveillance',
                        'baseline surveillance',
                        'Random',
                        'random surveillance',
                        'Surveillance',
                        'surveillance',
                        'Baseline Surveilliance',
                        'Active Surveillance',
                        'Routine Surveillance',
                        'Geographic Representativeness',
                        'Baseline',
                        'BaselineSurveillance',
                        'Representative sampling',
                        'baselinesurveillance',
                        'Random surveillance',
                        'Epidemiological surveilance (Random sampling)',
                        'Base Surveillance',
                        'Basellne surveillance',
                        'Baseline Sureillance',
                        'Systematic genomic surveillance'
                      );
                """;
                statement.execute(sql2);
                // Set sampling strategy for Swiss data
                String sql3 = """
                    update y_main_metadata_staging
                    set sampling_strategy = 'Baseline surveillance'
                    where
                      country = 'Switzerland'
                      and (
                        (submitting_lab = 'Department of Biosystems Science and Engineering, ETH Zürich'
                           and originating_lab in ('Viollier AG', 'labor team w AG'))
                        or (submitting_lab = 'HUG, Laboratory of Virology and the Health2030 Genome Center')
                      );
                """;
                statement.execute(sql3);
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
