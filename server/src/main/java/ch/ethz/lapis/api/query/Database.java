package ch.ethz.lapis.api.query;

import ch.ethz.lapis.api.exception.OutdatedDataVersionException;
import ch.ethz.lapis.util.PangoLineageAlias;
import ch.ethz.lapis.util.PangoLineageQueryConverter;
import ch.ethz.lapis.util.SeqCompressor;
import ch.ethz.lapis.util.ZstdSeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {

    public static class Columns {
        public static final String GENBANK_ACCESSION = "genbank_accession"; // String
        public static final String SRA_ACCESSION  = "sra_accession"; // String
        public static final String GISAID_EPI_ISL  = "gisaid_epi_isl"; // String
        public static final String STRAIN  = "strain"; // String
        public static final String DATE = "date"; // Date
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
        public static final String NEXTSTRAIN_CLADE = "nextstrain_clade"; // String
        public static final String GISAID_CLADE = "gisaid_clade"; // String
        public static final String ORIGINATING_LAB = "originating_lab"; // String
        public static final String SUBMITTING_LAB = "submitting_lab"; // String
    }

    public static final String[] ALL_COLUMNS = new String[] {
        Columns.GENBANK_ACCESSION, Columns.SRA_ACCESSION, Columns.GISAID_EPI_ISL, Columns.STRAIN,
        Columns.DATE, Columns.DATE_SUBMITTED, Columns.REGION, Columns.COUNTRY, Columns.DIVISION,
        Columns.LOCATION, Columns.REGION_EXPOSURE, Columns.COUNTRY_EXPOSURE, Columns.DIVISION_EXPOSURE,
        Columns.HOST, Columns.AGE, Columns.SEX, Columns.HOSPITALIZED, Columns.DIED, Columns.FULLY_VACCINATED,
        Columns.SAMPLING_STRATEGY, Columns.PANGO_LINEAGE, Columns.NEXTSTRAIN_CLADE, Columns.GISAID_CLADE,
        Columns.ORIGINATING_LAB, Columns.SUBMITTING_LAB
    };
    public static final String[] STRING_COLUMNS = new String[] {
        Columns.GENBANK_ACCESSION, Columns.SRA_ACCESSION, Columns.GISAID_EPI_ISL, Columns.STRAIN,
        Columns.REGION, Columns.COUNTRY, Columns.DIVISION, Columns.LOCATION, Columns.REGION_EXPOSURE,
        Columns.COUNTRY_EXPOSURE, Columns.DIVISION_EXPOSURE, Columns.HOST, Columns.SEX,
        Columns.SAMPLING_STRATEGY, Columns.PANGO_LINEAGE, Columns.NEXTSTRAIN_CLADE, Columns.GISAID_CLADE,
        Columns.ORIGINATING_LAB, Columns.SUBMITTING_LAB
    };
    public static final String[] DATE_COLUMNS = new String[] {
        Columns.DATE, Columns.DATE_SUBMITTED
    };
    public static final String[] INTEGER_COLUMNS = new String[] {
        Columns.AGE
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
    private final Map<String, Boolean[]> booleanColumns = new HashMap<>();

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
        if (booleanColumns.containsKey(columnName)) {
            return booleanColumns.get(columnName);
        }
        return null;
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
                List<PangoLineageAlias> aliases = getPangolinLineageAliases(conn);
                PangoLineageQueryConverter pangoLineageQueryConverter = new PangoLineageQueryConverter(aliases);
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
                        if (i % 50000 == 0) {
                            System.out.println("Loading data to in-memory database: " + i + "/" + numberRows);
                        }
                        for (String stringColumn : STRING_COLUMNS) {
                            String s = rs.getString(stringColumn);
                            database.stringColumns.get(stringColumn)[i] = s != null ? s.intern() : null;
                        }
                        for (String integerColumn : INTEGER_COLUMNS) {
                            database.integerColumns.get(integerColumn)[i] = rs.getObject(integerColumn, Integer.class);
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
