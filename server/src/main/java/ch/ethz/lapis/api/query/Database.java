package ch.ethz.lapis.api.query;

import ch.ethz.lapis.util.PangoLineageAlias;
import ch.ethz.lapis.util.PangoLineageQueryConverter;
import ch.ethz.lapis.util.ReferenceGenomeData;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.Utils;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Database {

    public static class Columns {
        public static final String GENBANK_ACCESSION = "genbank_accession"; // String
        public static final String SRA_ACCESSION  = "sra_accession"; // String
        public static final String GISAID_EPI_ISL  = "gisaid_epi_isl"; // String
        public static final String STRAIN  = "strain"; // String
        public static final String DATE = "date"; // Date
        public static final String YEAR = "year"; // Integer
        public static final String MONTH = "month"; // Integer
        public static final String DATE_SUBMITTED = "date_submitted"; // Date
        public static final String REGION = "region"; // String
        public static final String COUNTRY = "country"; // String
        public static final String DIVISION = "division"; // String
        public static final String LOCATION = "location"; // String
        public static final String REGION_EXPOSURE = "region_exposure"; // String
        public static final String COUNTRY_EXPOSURE = "country_exposure"; // String
        public static final String DIVISION_EXPOSURE = "division_exposure"; // String
        public static final String HOST = "host"; // String
        public static final String AGE = "age"; // Integer
        public static final String SEX = "sex"; // String
        public static final String HOSPITALIZED = "hospitalized"; // Boolean
        public static final String DIED = "died"; // Boolean
        public static final String FULLY_VACCINATED = "fully_vaccinated"; // Boolean
        public static final String SAMPLING_STRATEGY = "sampling_strategy"; // String
        public static final String PANGO_LINEAGE = "pango_lineage"; // String
        public static final String NEXTCLADE_PANGO_LINEAGE = "nextclade_pango_lineage"; // String
        public static final String NEXTSTRAIN_CLADE = "nextstrain_clade"; // String
        public static final String GISAID_CLADE = "gisaid_clade"; // String
        public static final String ORIGINATING_LAB = "originating_lab"; // String
        public static final String SUBMITTING_LAB = "submitting_lab"; // String
        public static final String NEXTCLADE_QC_OVERALL_SCORE = "nextclade_qc_overall_score"; // Float
        public static final String NEXTCLADE_QC_MISSING_DATA_SCORE = "nextclade_qc_missing_data_score"; // Float
        public static final String NEXTCLADE_QC_MIXED_SITES_SCORE = "nextclade_qc_mixed_sites_score"; // Float
        public static final String NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE = "nextclade_qc_private_mutations_score"; // Float
        public static final String NEXTCLADE_QC_SNP_CLUSTERS_SCORE = "nextclade_qc_snp_clusters_score"; // Float
        public static final String NEXTCLADE_QC_FRAME_SHIFTS_SCORE = "nextclade_qc_frame_shifts_score"; // Float
        public static final String NEXTCLADE_QC_STOP_CODONS_SCORE = "nextclade_qc_stop_codons_score"; // Float
        public static final String NEXTCLADE_COVERAGE = "nextclade_coverage"; // Float
    }

    private static class DataVersionChangedDuringFetching extends RuntimeException {}

    public static final String[] ALL_COLUMNS = new String[] {
        Columns.GENBANK_ACCESSION, Columns.SRA_ACCESSION, Columns.GISAID_EPI_ISL, Columns.STRAIN,
        Columns.DATE, Columns.YEAR, Columns.MONTH, Columns.DATE_SUBMITTED, Columns.REGION, Columns.COUNTRY,
        Columns.DIVISION, Columns.LOCATION, Columns.REGION_EXPOSURE, Columns.COUNTRY_EXPOSURE,
        Columns.DIVISION_EXPOSURE, Columns.HOST, Columns.AGE, Columns.SEX, Columns.HOSPITALIZED, Columns.DIED,
        Columns.FULLY_VACCINATED, Columns.SAMPLING_STRATEGY, Columns.PANGO_LINEAGE, Columns.NEXTSTRAIN_CLADE,
        Columns.GISAID_CLADE, Columns.ORIGINATING_LAB, Columns.SUBMITTING_LAB
    };
    public static final String[] STRING_COLUMNS = new String[] {
        Columns.GENBANK_ACCESSION, Columns.SRA_ACCESSION, Columns.GISAID_EPI_ISL, Columns.STRAIN,
        Columns.REGION, Columns.COUNTRY, Columns.DIVISION, Columns.LOCATION, Columns.REGION_EXPOSURE,
        Columns.COUNTRY_EXPOSURE, Columns.DIVISION_EXPOSURE, Columns.HOST, Columns.SEX,
        Columns.SAMPLING_STRATEGY, Columns.PANGO_LINEAGE, Columns.NEXTCLADE_PANGO_LINEAGE, Columns.NEXTSTRAIN_CLADE,
        Columns.GISAID_CLADE, Columns.ORIGINATING_LAB, Columns.SUBMITTING_LAB
    };
    public static final String[] DATE_COLUMNS = new String[] {
        Columns.DATE, Columns.DATE_SUBMITTED
    };
    public static final String[] INTEGER_COLUMNS = new String[] {
        Columns.AGE, Columns.YEAR, Columns.MONTH
    };
    public static final String[] FLOAT_COLUMNS = new String[] {
        Columns.NEXTCLADE_QC_OVERALL_SCORE, Columns.NEXTCLADE_QC_MISSING_DATA_SCORE,
        Columns.NEXTCLADE_QC_MIXED_SITES_SCORE, Columns.NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE,
        Columns.NEXTCLADE_QC_SNP_CLUSTERS_SCORE, Columns.NEXTCLADE_QC_FRAME_SHIFTS_SCORE,
        Columns.NEXTCLADE_QC_STOP_CODONS_SCORE, Columns.NEXTCLADE_COVERAGE
    };
    public static final String[] BOOLEAN_COLUMNS = new String[] {
        Columns.HOSPITALIZED, Columns.DIED, Columns.FULLY_VACCINATED
    };

    private static Database instance;

    private static final SeqCompressor columnarCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
    private final long dataVersion;
    private final int size;
    private final PangoLineageQueryConverter pangoLineageQueryConverter;

    // Sequences
    private final Map<Integer, byte[]> nucSequencesColumnarCompressed = new HashMap<>();
    private final Map<String, Map<Integer, byte[]>> aaSequencesColumnarCompressed = new HashMap<>();

    // Metadata
    private final Map<String, String[]> stringColumns = new HashMap<>();
    private final Map<String, Integer[]> integerColumns = new HashMap<>();
    private final Map<String, Float[]> floatColumns = new HashMap<>();
    private final Map<String, Boolean[]> booleanColumns = new HashMap<>();

    // Mutations and insertions
    private final MutationStore nucMutationStore;
    private final Map<String, MutationStore> aaMutationStores; // One store per gene
    private final InsertionStore nucInsertionStore;
    private final Map<String, InsertionStore> aaInsertionStores; // One store per gene

    private Database(
        long dataVersion,
        int size,
        PangoLineageQueryConverter pangoLineageQueryConverter
    ) {
        this.dataVersion = dataVersion;
        this.size = size;
        this.pangoLineageQueryConverter = pangoLineageQueryConverter;
        this.nucMutationStore = new MutationStore(size);
        this.nucInsertionStore = new InsertionStore();
        this.aaMutationStores = new HashMap<>();
        this.aaInsertionStores = new HashMap<>();
        for (String name : ReferenceGenomeData.getInstance().getGeneNames()) {
            this.aaMutationStores.put(name, new MutationStore(size));
            this.aaInsertionStores.put(name, new InsertionStore());
        }
    }


    public long getDataVersion() {
        return dataVersion;
    }


    public int size() {
        return size;
    }

    public PangoLineageQueryConverter getPangoLineageQueryConverter() {
        return pangoLineageQueryConverter;
    }


    public String[] getStringColumn(String columnName) {
        return stringColumns.get(columnName);
    }


    public Integer[] getIntColumn(String columnName) {
        return integerColumns.get(columnName);
    }


    public Float[] getFloatColumn(String columnName) {
        return floatColumns.get(columnName);
    }


    public Boolean[] getBoolColumn(String columnName) {
        return booleanColumns.get(columnName);
    }


    public Object[] getColumn(String columnName) {
        if (stringColumns.containsKey(columnName)) {
            return stringColumns.get(columnName);
        }
        if (integerColumns.containsKey(columnName)) {
            return integerColumns.get(columnName);
        }
        if (floatColumns.containsKey(columnName)) {
            return floatColumns.get(columnName);
        }
        if (booleanColumns.containsKey(columnName)) {
            return booleanColumns.get(columnName);
        }
        return null;
    }


    public MutationStore getNucMutationStore() {
        return nucMutationStore;
    }


    public Map<String, MutationStore> getAaMutationStores() {
        return aaMutationStores;
    }


    public InsertionStore getNucInsertionStore() {
        return nucInsertionStore;
    }


    public Map<String, InsertionStore> getAaInsertionStores() {
        return aaInsertionStores;
    }


    public char[] getNucArray(int position) {
        byte[] compressed = nucSequencesColumnarCompressed.get(position);
        if (compressed == null) {
            return null;
        }
        return columnarCompressor.decompress(compressed).toCharArray();
    }


    public char[] getAAArray(String gene, int position) {
        var geneMap = aaSequencesColumnarCompressed.get(gene.toLowerCase());
        if (geneMap == null) {
            return null;
        }
        byte[] compressed = geneMap.get(position);
        if (compressed == null) {
            return null;
        }
        return columnarCompressor.decompress(compressed).toCharArray();
    }


    public static List<PangoLineageAlias> getPangoLineageAliases(Connection conn) throws SQLException {
        String sql = """
            select
              alias,
              full_name
            from pangolin_lineage_alias;
        """;
        try (Statement statement = conn.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                List<PangoLineageAlias> aliases = new ArrayList<>();
                while (rs.next()) {
                    aliases.add(new PangoLineageAlias(
                        rs.getString("alias"),
                        rs.getString("full_name")
                    ));
                }
                return aliases;
            }
        }
    }


    public static void updateInstance(ComboPooledDataSource databasePool) {
        try {
            try {
                instance = loadDatabase(databasePool);
            } catch (DataVersionChangedDuringFetching ignored) {
                // If the data version changed during fetching, we retry once.
                // If the second time also fails, something is likely wrong.
                instance = loadDatabase(databasePool);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized Database getOrLoadInstance(ComboPooledDataSource databasePool) {
        if (instance == null) {
            updateInstance(databasePool);
        }
        return instance;
    }

    private static Database loadDatabase(ComboPooledDataSource databasePool) throws SQLException {
        try (Connection conn = databasePool.getConnection()) {
            String dataVersionSql = """
                select timestamp
                from data_version
                where dataset = 'merged';
                """;
            String lengthSql = """
                select count(*) as count
                from y_main_metadata
                """;
            Database database;
            int numberRows;
            try (Statement statement = conn.createStatement()) {
                // Fetch data version
                long dataVersion;
                try (ResultSet rs = statement.executeQuery(dataVersionSql)) {
                    if (!rs.next()) {
                        throw new RuntimeException("The data version cannot be found in the database.");
                    }
                    dataVersion = rs.getLong("timestamp");
                }
                // Fetch number of rows
                try (ResultSet rs = statement.executeQuery(lengthSql)) {
                    rs.next();
                    numberRows = rs.getInt("count");

                }
                // Fetch pango lineage aliases
                List<PangoLineageAlias> aliases = getPangoLineageAliases(conn);
                PangoLineageQueryConverter pangoLineageQueryConverter = new PangoLineageQueryConverter(aliases);
                // Create database object
                database = new Database(
                    dataVersion,
                    numberRows,
                    pangoLineageQueryConverter
                );
            }

            // Fill the database
            Thread t1 = new Thread(() -> {
                try {
                    Database.loadSequences(databasePool, database);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    Database.loadMetadata(databasePool, database, numberRows);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            Thread t3 = new Thread(() -> {
                try {
                    Database.loadMutationsAndInsertions(databasePool, database, numberRows);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();

            // Fetch the data version again and check whether it has changed in the meantime
            try (Statement statement = conn.createStatement()) {
                long dataVersion;
                try (ResultSet rs = statement.executeQuery(dataVersionSql)) {
                    if (!rs.next()) {
                        throw new RuntimeException("The data version cannot be found in the database.");
                    }
                    dataVersion = rs.getLong("timestamp");
                }
                if (dataVersion != database.getDataVersion()) {
                    System.err.println("Data version has changed. Initial version: " + database.getDataVersion() +
                        ", current version: " + dataVersion);
                    throw new DataVersionChangedDuringFetching();
                }
            }

            return database;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadSequences(ComboPooledDataSource databasePool, Database database)
        throws SQLException {
        String nucSequenceColumnarSql = """
                select position, data_compressed
                from y_main_sequence_columnar;
                """;
        String aaSequenceColumnarSql = """
                select gene, position, data_compressed
                from y_main_aa_sequence_columnar;
                """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.setFetchSize(2000);
                try (ResultSet rs = statement.executeQuery(nucSequenceColumnarSql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 5000 == 0) {
                            System.out.println(LocalDateTime.now() +
                                " Loading columnar nuc sequences to in-memory database: " + i + "/" + 29904);
                        }
                        int position = rs.getInt("position");
                        byte[] compressed = rs.getBytes("data_compressed");
                        database.nucSequencesColumnarCompressed.put(position, compressed);
                        i++;
                    }
                }
                try (ResultSet rs = statement.executeQuery(aaSequenceColumnarSql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 5000 == 0) {
                            System.out.println(LocalDateTime.now() +
                                " Loading columnar AA sequences to in-memory database: " + i + "/ ?");
                        }
                        String gene = rs.getString("gene").toLowerCase();
                        int position = rs.getInt("position");
                        byte[] compressed = rs.getBytes("data_compressed");
                        database.aaSequencesColumnarCompressed.putIfAbsent(gene, new HashMap<>());
                        database.aaSequencesColumnarCompressed.get(gene).put(position, compressed);
                        i++;
                    }
                }
            }
        }
    }

    private static void loadMetadata(ComboPooledDataSource databasePool, Database database, int numberRows)
        throws SQLException {
        String metadataSql = """
                select *
                from y_main_metadata
                order by id;
                """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                for (String stringColumn : STRING_COLUMNS) {
                    database.stringColumns.put(stringColumn, new String[numberRows]);
                }
                for (String integerColumn : INTEGER_COLUMNS) {
                    database.integerColumns.put(integerColumn, new Integer[numberRows]);
                }
                for (String floatColumn : FLOAT_COLUMNS) {
                    database.floatColumns.put(floatColumn, new Float[numberRows]);
                }
                for (String dateColumn : DATE_COLUMNS) {
                    database.integerColumns.put(dateColumn, new Integer[numberRows]);
                }
                for (String booleanColumn : BOOLEAN_COLUMNS) {
                    database.booleanColumns.put(booleanColumn, new Boolean[numberRows]);
                }
                statement.setFetchSize(20000);
                try (ResultSet rs = statement.executeQuery(metadataSql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 100000 == 0) {
                            System.out.println(LocalDateTime.now() +
                                " Loading metadata to in-memory database: " + i + "/" + numberRows);
                        }
                        for (String stringColumn : STRING_COLUMNS) {
                            String s = rs.getString(stringColumn);
                            database.stringColumns.get(stringColumn)[i] = s != null ? s.intern() : null;
                        }
                        for (String integerColumn : INTEGER_COLUMNS) {
                            database.integerColumns.get(integerColumn)[i] = rs.getObject(integerColumn, Integer.class);
                        }
                        for (String floatColumn : FLOAT_COLUMNS) {
                            database.floatColumns.get(floatColumn)[i] = Utils.nullableDoubleToFloat(
                                rs.getObject(floatColumn, Double.class));
                        }
                        for (String dateColumn : DATE_COLUMNS) {
                            Date dateObj = rs.getDate(dateColumn);
                            Integer dateInt = dateObj != null ? dateToInt(dateObj.toLocalDate()) : null;
                            database.integerColumns.get(dateColumn)[i] = dateInt;
                        }
                        for (String booleanColumn : BOOLEAN_COLUMNS) {
                            database.booleanColumns.get(booleanColumn)[i] = rs.getObject(booleanColumn, Boolean.class);
                        }
                        ++i;
                    }
                }
            }
        }
    }

    private static void loadMutationsAndInsertions(
        ComboPooledDataSource databasePool,
        Database database,
        int numberRows
    ) throws SQLException {
        String sequenceSql = """
            select
              id,
              coalesce(aa_mutations, '') as aa_mutations,
              coalesce(aa_insertions, '') as aa_insertions,
              coalesce(aa_unknowns, '') as aa_unknowns,
              coalesce(nuc_substitutions, '') as nuc_substitutions,
              coalesce(nuc_deletions, '') as nuc_deletions,
              coalesce(nuc_insertions, '') as nuc_insertions,
              coalesce(nuc_unknowns, '') as nuc_unknowns
            from y_main_sequence
            order by id;
            """;
        try (Connection conn = databasePool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.setFetchSize(20000);
                try (ResultSet rs = statement.executeQuery(sequenceSql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 100000 == 0) {
                            System.out.println(LocalDateTime.now() +
                                " Loading mutations (and insertions) to in-memory database: " + i + "/" + numberRows);
                        }
                        int id = rs.getInt("id");
                        // Nuc mutations
                        String nucMutationsString = rs.getString("nuc_substitutions") +
                            "," + rs.getString("nuc_deletions");
                        List<MutationStore.Mutation> nucMutations = new ArrayList<>();
                        for (String mut : nucMutationsString.split(",")) {
                            if (mut.isBlank() || mut.equals("null")) {
                                continue;
                            }
                            nucMutations.add(MutationStore.Mutation.parse(mut));
                        }
                        String nucUnknownsString = rs.getString("nuc_unknowns");
                        List<String> nucUnknowns = Arrays.stream(nucUnknownsString.split(","))
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList());
                        database.nucMutationStore.putEntry(id, nucMutations, nucUnknowns);
                        // AA mutations
                        String aaMutationsString = rs.getString("aa_mutations");
                        String aaUnknownsString = rs.getString("aa_unknowns");
                        Map<String, List<MutationStore.Mutation>> aaMutationsPerGene = new HashMap<>();
                        Map<String, List<String>> aaUnknownsPerGene = new HashMap<>();
                        for (String name : ReferenceGenomeData.getInstance().getGeneNames()) {
                            aaMutationsPerGene.put(name, new ArrayList<>());
                            aaUnknownsPerGene.put(name, new ArrayList<>());
                        }
                        for (String mutWithGene : aaMutationsString.split(",")) {
                            if (mutWithGene.isBlank()) {
                                continue;
                            }
                            String[] parts = mutWithGene.split(":");
                            String gene = parts[0];
                            String mut = parts[1];
                            aaMutationsPerGene.get(gene).add(MutationStore.Mutation.parse(mut));
                        }
                        Arrays.stream(aaUnknownsString.split(","))
                            .filter(s -> !s.isBlank())
                            .forEach(unknownsWithGene -> {
                                String[] parts = unknownsWithGene.split(":");
                                String gene = parts[0];
                                String unknowns = parts[1];
                                aaUnknownsPerGene.get(gene).add(unknowns);
                            });
                        database.aaMutationStores.forEach((gene, mutationStore) -> {
                            mutationStore.putEntry(id, aaMutationsPerGene.get(gene), aaUnknownsPerGene.get(gene));
                        });
                        // Nuc insertions
                        String nucInsertionsString = rs.getString("nuc_insertions");
                        if (!nucInsertionsString.isBlank()) {
                            Arrays.stream(nucInsertionsString.split(","))
                                .forEach(ins -> database.nucInsertionStore.putInsertions(id, ins));
                        }
                        // AA insertions
                        String aaInsertionsString = rs.getString("aa_insertions");
                        if (!aaInsertionsString.isBlank()) {
                            Arrays.stream(aaInsertionsString.split(","))
                                .forEach(geneIns -> {
                                    String[] split = geneIns.split(":", 2);
                                    String gene = split[0];
                                    String ins = split[1];
                                    database.aaInsertionStores.get(gene).putInsertions(id, ins);
                                });
                        }
                        // Done
                        ++i;
                    }
                }
            }
        }
    }

    public static Integer dateToInt(LocalDate date) {
        return date != null ? (int) date.toEpochDay() : null;
    }

    public static LocalDate intToDate(Integer dateInt) {
        return dateInt != null ? LocalDate.ofEpochDay(dateInt) : null;
    }
}
