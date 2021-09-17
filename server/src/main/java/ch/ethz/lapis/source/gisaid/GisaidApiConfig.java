package ch.ethz.lapis.source.gisaid;

import ch.ethz.lapis.core.Config;


public class GisaidApiConfig implements Config {
    private String url;
    private String username;
    private String password;

    public String getUrl() {
        return url;
    }

    public GisaidApiConfig setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public GisaidApiConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public GisaidApiConfig setPassword(String password) {
        this.password = password;
        return this;
    }
}
