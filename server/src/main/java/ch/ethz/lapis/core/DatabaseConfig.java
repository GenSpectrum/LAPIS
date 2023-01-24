package ch.ethz.lapis.core;

import lombok.Data;

@Data
public class DatabaseConfig implements Config {
    private String host;
    private int port;
    private String dbname;
    private String username;
    private String password;
    private String schema;
    private int maxPoolSize = 30;
}
