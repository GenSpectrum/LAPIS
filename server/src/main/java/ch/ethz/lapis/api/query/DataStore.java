package ch.ethz.lapis.api.query;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.util.DeflateSeqCompressor;
import ch.ethz.lapis.util.SeqCompressor;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataStore {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private static final SeqCompressor nucMutationColumnarCompressor
        = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.ATCGNDEL);
    private static final SeqCompressor aaMutationColumnarCompressor
        = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.AACODONS);

    private String[] pangoLineageArray;
    private String[] nextstrainCladeArray;
    private String[] gisaidCladeArray;


    public DataStore() {
        loadLineageNames();
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
                    return nucMutationColumnarCompressor.decompress(compressed).toCharArray();
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
                    return aaMutationColumnarCompressor.decompress(compressed).toCharArray();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
