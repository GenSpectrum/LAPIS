package ch.ethz.y.core;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.yaml.snakeyaml.Yaml;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;


public class DatabaseService {

    public static Connection openDatabaseConnection(String instance) {
        return openDatabaseConnection(instance, Path.of("config.yml"));
    }


    public static Connection openDatabaseConnection(String instance, Path configPath) {
        // Read config file
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configPath.toAbsolutePath().toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Load database configs from config file
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);
        Map<String, Map<String, Object>> databaseConfigs = (Map<String, Map<String, Object>>)
                ((Map<String, Object>) config.get("default")).get("database");
        if (!databaseConfigs.containsKey(instance)) {
            throw new RuntimeException("Configs for database instance \"" + instance + "\" cannot be found.");
        }
        Map<String, Object> databaseConfig = databaseConfigs.get(instance);
        String host = (String) databaseConfig.get("host");
        int port = (int) databaseConfig.get("port");
        String username = (String) databaseConfig.get("username");
        String dbname = (String) databaseConfig.get("dbname");
        String password;
        if (databaseConfig.containsKey("password")) {
            password = (String) databaseConfig.get("password");
        } else {
            password = new String(System.console()
                    .readPassword("Please enter the password for user " + username + ":"));
        }

        // Create database connection
        try {
            return DriverManager
                    .getConnection("jdbc:postgresql://" + host + ":" + port + "/" + dbname, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static Connection openDatabaseConnection(DatabaseConfig config) {
        String host = config.getHost();
        int port = config.getPort();
        String username = config.getUsername();
        String dbname = config.getDbname();
        String password = config.getPassword();
        try {
            return DriverManager
                    .getConnection("jdbc:postgresql://" + host + ":" + port + "/" + dbname, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static ComboPooledDataSource createDatabaseConnectionPool(String instance) {
        return createDatabaseConnectionPool(instance, Path.of("config.yml"));
    }


    public static ComboPooledDataSource createDatabaseConnectionPool(String instance, Path configPath) {
        // Read config file
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configPath.toAbsolutePath().toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Load database configs from config file
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);
        Map<String, Map<String, Object>> databaseConfigs = (Map<String, Map<String, Object>>)
                ((Map<String, Object>) config.get("default")).get("database");
        if (!databaseConfigs.containsKey(instance)) {
            throw new RuntimeException("Configs for database instance \"" + instance + "\" cannot be found.");
        }
        Map<String, Object> databaseConfig = databaseConfigs.get(instance);
        String host = (String) databaseConfig.get("host");
        int port = (int) databaseConfig.get("port");
        String username = (String) databaseConfig.get("username");
        String dbname = (String) databaseConfig.get("dbname");
        String password = (String) databaseConfig.get("password");

        // Create pool
        try {
            ComboPooledDataSource pool = new ComboPooledDataSource();
            pool.setDriverClass("org.postgresql.Driver");
            pool.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbname);
            pool.setUser(username);
            pool.setPassword(password);
            return pool;
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }
    }


    public static ComboPooledDataSource createDatabaseConnectionPool(DatabaseConfig config) {
        String host = config.getHost();
        int port = config.getPort();
        String username = config.getUsername();
        String dbname = config.getDbname();
        String password = config.getPassword();
        try {
            ComboPooledDataSource pool = new ComboPooledDataSource();
            pool.setDriverClass("org.postgresql.Driver");
            pool.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbname);
            pool.setUser(username);
            pool.setPassword(password);
            return pool;
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }
    }

}
