package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.query.DataStore;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;


@Service
public class DataVersionService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private final Optional<CacheService> cacheServiceOpt;
    private final DataStore dataStore;

    private long version = -1;


    public DataVersionService(Optional<CacheService> cacheServiceOpt, DataStore dataStore) {
        this.cacheServiceOpt = cacheServiceOpt;
        this.dataStore = dataStore;
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
                        version = newVersion;
                        if (cacheServiceOpt.isPresent()) {
                            cacheServiceOpt.get().updateCacheIfOutdated(version);
                        }
                        dataStore.loadLineageNames();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
