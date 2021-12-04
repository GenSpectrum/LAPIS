package ch.ethz.lapis.api.query;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataStore {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private static final SeqCompressor columnarCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
    private final PangoLineageQueryConverter pangoLineageQueryConverter;

    private String[] pangoLineageArray;
    private String[] nextstrainCladeArray;
    private String[] gisaidCladeArray;


    public DataStore() {
        loadLineageNames();
        List<PangoLineageAlias> pangolinLineageAliases = getPangolinLineageAliases();
        this.pangoLineageQueryConverter = new PangoLineageQueryConverter(pangolinLineageAliases);
    }

    public synchronized void loadLineageNames() {
        String sql = """
            select
                upper(pango_lineage) as pango_lineage,
                upper(nextstrain_clade) as nextstrain_clade,
                upper(gisaid_clade) as gisaid_clade
            from y_main_metadata
            order by id;
        """;
        List<String> pangoLineageList = new ArrayList<>();
        List<String> nextstrainCladeList = new ArrayList<>();
        List<String> gisaidCladeList = new ArrayList<>();
        try (Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    while (rs.next()) {
                        pangoLineageList.add(rs.getString("pango_lineage"));
                        nextstrainCladeList.add(rs.getString("nextstrain_clade"));
                        gisaidCladeList.add(rs.getString("gisaid_clade"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.pangoLineageArray = pangoLineageList.toArray(new String[0]);
        this.nextstrainCladeArray = nextstrainCladeList.toArray(new String[0]);
        this.gisaidCladeArray = gisaidCladeList.toArray(new String[0]);
    }


    private List<PangoLineageAlias> getPangolinLineageAliases() {
        String sql = """
            select
              alias,
              full_name
            from pangolin_lineage_alias;
        """;
        try (Connection conn = dbPool.getConnection()) {
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public PangoLineageQueryConverter getPangoLineageQueryConverter() {
        return pangoLineageQueryConverter;
    }


    public String[] getPangoLineageArray() {
        if (pangoLineageArray == null) {
            loadLineageNames();
        }
        return pangoLineageArray;
    }


    public String[] getNextstrainCladeArray() {
        if (pangoLineageArray == null) {
            loadLineageNames();
        }
        return nextstrainCladeArray;
    }

    public String[] getGisaidCladeArray() {
        if (pangoLineageArray == null) {
            loadLineageNames();
        }
        return gisaidCladeArray;
    }

    public char[] getNucArray(int position) {
        String sql = """
            select data_compressed
            from y_main_sequence_columnar
            where position = ?;
        """;
        try (Connection conn = dbPool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, position);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    byte[] compressed = rs.getBytes("data_compressed");
                    return columnarCompressor.decompress(compressed).toCharArray();
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
        try (Connection conn = dbPool.getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, gene);
                statement.setInt(2, position);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    byte[] compressed = rs.getBytes("data_compressed");
                    return columnarCompressor.decompress(compressed).toCharArray();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
