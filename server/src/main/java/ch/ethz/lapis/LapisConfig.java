package ch.ethz.lapis;

import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.core.Config;
import ch.ethz.lapis.core.DatabaseConfig;
import ch.ethz.lapis.core.HttpProxyConfig;
import ch.ethz.lapis.source.gisaid.GisaidApiConfig;

public class LapisConfig implements Config {

    private DatabaseConfig vineyard;
    private HttpProxyConfig httpProxy;
    private String workdir;
    private Integer maxNumberWorkers;
    private String nextalignPath;
    private String nextcladePath;
    private String geoLocationRulesPath;
    private GisaidApiConfig gisaidApiConfig;
    private Boolean cacheEnabled;
    private String redisHost;
    private Integer redisPort;
    private OpennessLevel apiOpennessLevel;
    private DatabaseConfig s3cVineyard;

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

    public String getNextcladePath() {
        return nextcladePath;
    }

    public LapisConfig setNextcladePath(String nextcladePath) {
        this.nextcladePath = nextcladePath;
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

    public Boolean getCacheEnabled() {
        return cacheEnabled;
    }

    public LapisConfig setCacheEnabled(Boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        return this;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public LapisConfig setRedisHost(String redisHost) {
        this.redisHost = redisHost;
        return this;
    }

    public Integer getRedisPort() {
        return redisPort;
    }

    public LapisConfig setRedisPort(Integer redisPort) {
        this.redisPort = redisPort;
        return this;
    }

    public OpennessLevel getApiOpennessLevel() {
        return apiOpennessLevel;
    }

    public LapisConfig setApiOpennessLevel(OpennessLevel apiOpennessLevel) {
        this.apiOpennessLevel = apiOpennessLevel;
        return this;
    }

    public DatabaseConfig getS3cVineyard() {
        return s3cVineyard;
    }

    public LapisConfig setS3cVineyard(DatabaseConfig s3cVineyard) {
        this.s3cVineyard = s3cVineyard;
        return this;
    }

    public enum Source {
        NG, GISAID, MPOX
    }
}
