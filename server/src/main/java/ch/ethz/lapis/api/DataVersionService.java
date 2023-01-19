package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.query.Database;
import ch.ethz.lapis.util.PangoLineageAlias;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;


@Service
public class DataVersionService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private final Optional<CacheService> cacheServiceOpt;

    private long version = -1;


    public DataVersionService(Optional<CacheService> cacheServiceOpt) {
        this.cacheServiceOpt = cacheServiceOpt;
        autoFetchVersionDate();
    }


    public long getVersion() {
        return version;
    }


    @Scheduled(fixedDelay = 1000)
    public void autoFetchVersionDate() {
        String sql = """
                select timestamp
                from data_version
                where dataset = 'merged';
            """;
        try (Connection conn = dbPool.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    if (!rs.next()) {
                        throw new RuntimeException("The data version cannot be found in the database.");
                    }
                    long newVersion = rs.getLong("timestamp");
                    if (newVersion != version) {
                        // Update the cache
                        System.out.println("New data version: " + version + " -> " + newVersion);
                        if (version == -1) {
                            // Initial loading. Using getOrLoadInstance() prevents the data to be loaded in parallel,
                            // e.g., by SampleController.
                            Database.getOrLoadInstance(dbPool);
                        } else {
                            Database.updateInstance(dbPool);
                        }
                        version = newVersion;
                        cacheServiceOpt.ifPresent(cacheService -> cacheService.updateCacheIfOutdated(version));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Scheduled(fixedDelay = 60000)
    public void autoFetchPangoAliases() {
        Database database = Database.getOrLoadInstance(dbPool);
        try (Connection conn = dbPool.getConnection()) {
            List<PangoLineageAlias> aliases = Database.getPangoLineageAliases(conn);
            database.getPangoLineageQueryConverter().updateAliases(aliases);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
