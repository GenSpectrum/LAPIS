package ch.ethz.lapis.core;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;


public class DatabaseService {

    public static ComboPooledDataSource createDatabaseConnectionPool(DatabaseConfig config) {
        String host = config.getHost();
        int port = config.getPort();
        String username = config.getUsername();
        String dbname = config.getDbname();
        String password = config.getPassword();
        String schema = config.getSchema();
        try {
            ComboPooledDataSource pool = new ComboPooledDataSource();
            pool.setDriverClass("org.postgresql.Driver");
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
            if (schema != null && !schema.isBlank()) {
                jdbcUrl += "?currentSchema=" + schema;
            }
            pool.setJdbcUrl(jdbcUrl);
            pool.setUser(username);
            pool.setPassword(password);
            pool.setMaxPoolSize(config.getMaxPoolSize());
            return pool;
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }
    }

}
