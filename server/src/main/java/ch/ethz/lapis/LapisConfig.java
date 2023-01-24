package ch.ethz.lapis;

import ch.ethz.lapis.api.entity.OpennessLevel;
import ch.ethz.lapis.core.Config;
import ch.ethz.lapis.core.DatabaseConfig;
import ch.ethz.lapis.core.HttpProxyConfig;
import ch.ethz.lapis.source.gisaid.GisaidApiConfig;
import lombok.Data;

@Data
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

    private String notificationKey;

    public enum Source {
        NG, GISAID
    }
}
