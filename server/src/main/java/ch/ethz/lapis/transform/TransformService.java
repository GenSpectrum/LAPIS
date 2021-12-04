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
import org.javatuples.Triplet;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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
            pullFromNextstrainGenbankTable();
        } else if (source == LapisConfig.Source.GISAID) {
            pullFromGisaidTable();
        }
        // Compress AA sequences
        compressAASeqs();
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


    public void compressAASeqs() throws SQLException, InterruptedException {
        // We will use a Triplet to store the elements.
        // Triplet<Integer, String, String> = (id, gene, aa_seq)

        // Fetch the uncompressed AA sequences
        ExhaustibleBlockingQueue<List<Triplet<Integer, String, String>>> batches;
        String fetchSql = """
            select id, gene, aa_seq
            from y_main_aa_sequence_staging;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(fetchSql)) {
                var elementQueue = new DatabaseReaderQueueBuilder<>(
                    statement,
                    (rs) -> {
                        try {
                            return new Triplet<>(rs.getInt("id"), rs.getString("gene"), rs.getString("aa_seq"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    3000, 3000
                ).build();
                batches = ExhaustibleLinkedBlockingQueue.batchElements(elementQueue, 1000, 30);
            }
        }

        // Compress and write to database
        ExecutorService executor = Executors.newFixedThreadPool(maxNumberWorkers);
        for (int i = 0; i < maxNumberWorkers; i++) {
            executor.submit(() -> {
                try {
                    while (!batches.isEmpty() || !batches.isExhausted()) {
                        // Get a batch from the queue
                        List<Triplet<Integer, String, String>> batch = batches.poll(3, TimeUnit.SECONDS);
                        if (batch == null) {
                            continue;
                        }
                        // Compress
                        List<Triplet<Integer, String, byte[]>> compressed = new ArrayList<>();
                        for (Triplet<Integer, String, String> entry : batch) {
                            compressed.add(new Triplet<>(
                                entry.getValue0(),
                                entry.getValue1(),
                                aaSeqCompressor.compress(entry.getValue2())
                            ));
                        }
                        // Write to database
                        String insertSql = """
                            update y_main_aa_sequence_staging
                            set aa_seq_compressed = ?
                            where id = ? and gene = ?;
                            """;
                        try (Connection conn = databasePool.getConnection()) {
                            conn.setAutoCommit(false);
                            try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
                                for (Triplet<Integer, String, byte[]> entry : compressed) {
                                    statement.setBytes(1, entry.getValue2());
                                    statement.setInt(2, entry.getValue0());
                                    statement.setString(3, entry.getValue1());
                                    statement.addBatch();
                                }
                                Utils.executeClearCommitBatch(conn, statement);
                            }
                            conn.setAutoCommit(true);
                        }
                    }
                } catch (InterruptedException | SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.DAYS);
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
        System.out.println(compressedSequences.size());
        System.out.println("Data loaded");

        SequenceRowToColumnTransformer transformer = new SequenceRowToColumnTransformer(maxNumberWorkers, 2500);
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
            System.out.println(LocalDateTime.now() + "Gene " + gene + " - Start processing");
            // Load all amino acid sequences and their IDs
            List<String> aaSequences = new ArrayList<>();
            int idCounter = 0;
            try (Connection conn = databasePool.getConnection()) {
                try (PreparedStatement statement = conn.prepareStatement(sql1)) {
                    statement.setString(1, gene);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String seq = rs.getString("aa_seq");
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

            SequenceRowToColumnTransformer transformer = new SequenceRowToColumnTransformer(maxNumberWorkers, 2500);
            transformer.transform(
                aaSequences,
                (a) -> a,
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
                // Change pangoLineage "None" to null
                String sql1 = """
                    update y_main_metadata_staging m
                    set pango_lineage = null
                    where m.pango_lineage = 'None';
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
