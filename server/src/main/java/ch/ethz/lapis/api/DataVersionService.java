package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


@Service
public class DataVersionService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private final CacheService cacheService;

    private long version = -1;


    public DataVersionService(CacheService cacheService) {
        this.cacheService = cacheService;
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
                        cacheService.updateCacheIfOutdated(version);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}