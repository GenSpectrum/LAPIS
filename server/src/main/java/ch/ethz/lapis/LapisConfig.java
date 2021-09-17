package ch.ethz.lapis;

import ch.ethz.lapis.core.Config;
import ch.ethz.lapis.core.DatabaseConfig;
import ch.ethz.lapis.core.HttpProxyConfig;
import ch.ethz.lapis.source.gisaid.GisaidApiConfig;

public class LapisConfig implements Config {

    public enum Source {
        NG, GISAID;
    }

    private DatabaseConfig vineyard;

    private HttpProxyConfig httpProxy;

    private String workdir;

    private Integer maxNumberWorkers;

    private String nextalignPath;

    private String geoLocationRulesPath;

    private GisaidApiConfig gisaidApiConfig;

    private Source source;


    public DatabaseConfig getVineyard() {
        return vineyard;
    }

    public LapisConfig setVineyard(DatabaseConfig vineyard) {
        this.vineyard = vineyard;
        return this;
    }

    public HttpProxyConfig getHttpProxy() {
        return httpProxy;
    }

    public LapisConfig setHttpProxy(HttpProxyConfig httpProxy) {
        this.httpProxy = httpProxy;
        return this;
    }

    public String getWorkdir() {
        return workdir;
    }

    public LapisConfig setWorkdir(String workdir) {
        this.workdir = workdir;
        return this;
    }

    public Integer getMaxNumberWorkers() {
        return maxNumberWorkers;
    }

    public LapisConfig setMaxNumberWorkers(Integer maxNumberWorkers) {
        this.maxNumberWorkers = maxNumberWorkers;
        return this;
    }

    public String getNextalignPath() {
        return nextalignPath;
    }

    public LapisConfig setNextalignPath(String nextalignPath) {
        this.nextalignPath = nextalignPath;
        return this;
    }

    public String getGeoLocationRulesPath() {
        return geoLocationRulesPath;
    }

    public LapisConfig setGeoLocationRulesPath(String geoLocationRulesPath) {
        this.geoLocationRulesPath = geoLocationRulesPath;
        return this;
    }

    public GisaidApiConfig getGisaidApiConfig() {
        return gisaidApiConfig;
    }

    public LapisConfig setGisaidApiConfig(GisaidApiConfig gisaidApiConfig) {
        this.gisaidApiConfig = gisaidApiConfig;
        return this;
    }

    public Source getSource() {
        return source;
    }

    public LapisConfig setSource(Source source) {
        this.source = source;
        return this;
    }
}
