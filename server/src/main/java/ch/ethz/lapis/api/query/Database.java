package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.OutdatedDataVersionException;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Database {

    public static class Columns {
        public static final String ACCESSION = "accession"; // String
        public static final String SRA_ACCESSION  = "sra_accession"; // String
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
        public static final String CLADE = "clade"; // String
        public static final String LINEAGE = "lineage"; // String
        public static final String INSTITUTION = "institution"; // String
        public static final String NEXTCLADE_QC_OVERALL_SCORE = "nextclade_qc_overall_score"; // Float
        public static final String NEXTCLADE_QC_MISSING_DATA_SCORE = "nextclade_qc_missing_data_score"; // Float
        public static final String NEXTCLADE_QC_MIXED_SITES_SCORE = "nextclade_qc_mixed_sites_score"; // Float
        public static final String NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE = "nextclade_qc_private_mutations_score"; // Float
        public static final String NEXTCLADE_QC_SNP_CLUSTERS_SCORE = "nextclade_qc_snp_clusters_score"; // Float
        public static final String NEXTCLADE_QC_FRAME_SHIFTS_SCORE = "nextclade_qc_frame_shifts_score"; // Float
        public static final String NEXTCLADE_QC_STOP_CODONS_SCORE = "nextclade_qc_stop_codons_score"; // Float
        public static final String NEXTCLADE_ALIGNMENT_SCORE = "nextclade_alignment_score"; // Float
        public static final String NEXTCLADE_ALIGNMENT_START = "nextclade_alignment_start"; // Integer
        public static final String NEXTCLADE_ALIGNMENT_END = "nextclade_alignment_end"; // Integer
        public static final String NEXTCLADE_TOTAL_SUBSTITUTIONS = "nextclade_total_substitutions"; // Integer
        public static final String NEXTCLADE_TOTAL_DELETIONS = "nextclade_total_deletions"; // Integer
        public static final String NEXTCLADE_TOTAL_INSERTIONS = "nextclade_total_insertions"; // Integer
        public static final String NEXTCLADE_TOTAL_FRAME_SHIFTS = "nextclade_total_frame_shifts"; // Integer
        public static final String NEXTCLADE_TOTAL_AMINOACID_SUBSTITUTIONS = "nextclade_total_aminoacid_substitutions"; // Integer
        public static final String NEXTCLADE_TOTAL_AMINOACID_DELETIONS = "nextclade_total_aminoacid_deletions"; // Integer
        public static final String NEXTCLADE_TOTAL_AMINOACID_INSERTIONS = "nextclade_total_aminoacid_insertions"; // Integer
        public static final String NEXTCLADE_TOTAL_MISSING = "nextclade_total_missing"; // Integer
        public static final String NEXTCLADE_TOTAL_NON_ACGTNS = "nextclade_total_non_acgtns"; // Integer
        public static final String NEXTCLADE_TOTAL_PCR_PRIMER_CHANGES = "nextclade_total_pcr_primer_changes"; // Integer

    }
    public static final String[] STRING_COLUMNS = new String[] {
        Columns.ACCESSION, Columns.SRA_ACCESSION, Columns.STRAIN,
        Columns.REGION, Columns.COUNTRY, Columns.DIVISION, Columns.LOCATION, Columns.REGION_EXPOSURE,
        Columns.COUNTRY_EXPOSURE, Columns.DIVISION_EXPOSURE, Columns.HOST, Columns.SEX,
        Columns.SAMPLING_STRATEGY, Columns.CLADE, Columns.LINEAGE, Columns.INSTITUTION
    };
    public static final String[] DATE_COLUMNS = new String[] {
        Columns.DATE, Columns.DATE_SUBMITTED
    };
    public static final String[] INTEGER_COLUMNS = new String[] {
        Columns.AGE, Columns.YEAR, Columns.MONTH,
        Columns.NEXTCLADE_ALIGNMENT_START, Columns.NEXTCLADE_ALIGNMENT_END, Columns.NEXTCLADE_TOTAL_SUBSTITUTIONS,
        Columns.NEXTCLADE_TOTAL_DELETIONS, Columns.NEXTCLADE_TOTAL_INSERTIONS, Columns.NEXTCLADE_TOTAL_FRAME_SHIFTS,
        Columns.NEXTCLADE_TOTAL_AMINOACID_SUBSTITUTIONS, Columns.NEXTCLADE_TOTAL_AMINOACID_DELETIONS,
        Columns.NEXTCLADE_TOTAL_AMINOACID_INSERTIONS, Columns.NEXTCLADE_TOTAL_MISSING,
        Columns.NEXTCLADE_TOTAL_NON_ACGTNS, Columns.NEXTCLADE_TOTAL_PCR_PRIMER_CHANGES
    };
    public static final String[] FLOAT_COLUMNS = new String[] {
        Columns.NEXTCLADE_QC_OVERALL_SCORE, Columns.NEXTCLADE_QC_MISSING_DATA_SCORE,
        Columns.NEXTCLADE_QC_MIXED_SITES_SCORE, Columns.NEXTCLADE_QC_PRIVATE_MUTATIONS_SCORE,
        Columns.NEXTCLADE_QC_SNP_CLUSTERS_SCORE, Columns.NEXTCLADE_QC_FRAME_SHIFTS_SCORE,
        Columns.NEXTCLADE_QC_STOP_CODONS_SCORE, Columns.NEXTCLADE_ALIGNMENT_SCORE
    };
    public static final String[] BOOLEAN_COLUMNS = new String[] {
        Columns.HOSPITALIZED, Columns.DIED, Columns.FULLY_VACCINATED
    };

    private static Database instance;

    private static final SeqCompressor columnarCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
    private final long dataVersion;
    private final int size;
    private final PangoLineageQueryConverter pangoLineageQueryConverter;
    private final ComboPooledDataSource databasePool;
    private final Map<String, String[]> stringColumns = new HashMap<>();
    private final Map<String, Integer[]> integerColumns = new HashMap<>();
    private final Map<String, Float[]> floatColumns = new HashMap<>();
    private final Map<String, Boolean[]> booleanColumns = new HashMap<>();
    private final MutationStore nucMutationStore;
    private final Map<String, MutationStore> aaMutationStores; // One store per gene

    private Database(
        long dataVersion,
        int size,
        ComboPooledDataSource databasePool,
        PangoLineageQueryConverter pangoLineageQueryConverter
    ) {
        this.dataVersion = dataVersion;
        this.size = size;
        this.databasePool = databasePool;
        this.pangoLineageQueryConverter = pangoLineageQueryConverter;
        this.nucMutationStore = new MutationStore(size);
        this.aaMutationStores = new HashMap<>();
        for (String name : ReferenceGenomeData.getInstance().getGeneNames()) {
            this.aaMutationStores.put(name, new MutationStore(size));
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


    private static List<PangoLineageAlias> getPangolinLineageAliases(Connection conn) throws SQLException {
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


    public char[] getNucArray(int position) {
        String sql = """
            select data_compressed
            from y_main_sequence_columnar
            where position = ?;
        """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, position);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    byte[] compressed = rs.getBytes("data_compressed");
                    char[] result = columnarCompressor.decompress(compressed).toCharArray();
                    if (result.length != size) {
                        // New data arrived. The available sequence data does not match the current database anymore.
                        throw new OutdatedDataVersionException();
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public char[] getAAArray(String gene, int position) {
        String sql = """
            select data_compressed
            from y_main_aa_sequence_columnar
            where lower(gene) = lower(?) and position = ?;
        """;
        try (Connection conn = databasePool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, gene);
                statement.setInt(2, position);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    byte[] compressed = rs.getBytes("data_compressed");
                    char[] result = columnarCompressor.decompress(compressed).toCharArray();
                    if (result.length != size) {
                        // New data arrived. The available sequence data does not match the current database anymore.
                        throw new OutdatedDataVersionException();
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateInstance(ComboPooledDataSource databasePool) {
        try {
            instance = loadDatabase(databasePool);
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
            conn.setAutoCommit(false);
            String dataVersionSql = """
                select timestamp
                from data_version
                where dataset = 'merged';
                """;
            String lengthSql = """
                select count(*) as count
                from y_main_metadata
                """;
            String metadataSql = """
                select *
                from y_main_metadata
                order by id;
                """;
            String sequenceSql = """
                select
                  id,
                  coalesce(aa_mutations, '') as aa_mutations,
                  coalesce(aa_unknowns, '') as aa_unknowns,
                  coalesce(nuc_substitutions, '') as nuc_substitutions,
                  coalesce(nuc_deletions, '') as nuc_deletions,
                  coalesce(nuc_unknowns, '') as nuc_unknowns
                from y_main_sequence;
                """;
            Database database;
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
                int numberRows;
                try (ResultSet rs = statement.executeQuery(lengthSql)) {
                    rs.next();
                    numberRows = rs.getInt("count");

                }
                // Fetch pango lineage aliases
                PangoLineageQueryConverter pangoLineageQueryConverter = new PangoLineageQueryConverter(new ArrayList<>());
                // Create database object
                database = new Database(
                    dataVersion,
                    numberRows,
                    databasePool,
                    pangoLineageQueryConverter
                );
                // Fetch metadata
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
                // Fetch mutations
                statement.setFetchSize(20000);
                try (ResultSet rs = statement.executeQuery(sequenceSql)) {
                    int i = 0;
                    while (rs.next()) {
                        if (i % 100000 == 0) {
                            System.out.println(LocalDateTime.now() +
                                " Loading mutations to in-memory database: " + i + "/" + numberRows);
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
                        ++i;
                    }
                }

            }
            conn.setAutoCommit(true);
            return database;
        }
    }

    public static Integer dateToInt(LocalDate date) {
        return date != null ? (int) date.toEpochDay() : null;
    }

    public static LocalDate intToDate(Integer dateInt) {
        return dateInt != null ? LocalDate.ofEpochDay(dateInt) : null;
    }
}
