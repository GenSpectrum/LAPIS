package ch.ethz.y;

import ch.ethz.y.core.Config;
import ch.ethz.y.core.DatabaseConfig;

public class YCoVDBConfig implements Config {

    private DatabaseConfig vineyard;

    private String workdir;

    private Integer maxNumberWorkers;

    private String nextalignPath;

    public DatabaseConfig getVineyard() {
        return vineyard;
    }

    public YCoVDBConfig setVineyard(DatabaseConfig vineyard) {
        this.vineyard = vineyard;
        return this;
    }

    public String getWorkdir() {
        return workdir;
    }

    public YCoVDBConfig setWorkdir(String workdir) {
        this.workdir = workdir;
        return this;
    }

    public Integer getMaxNumberWorkers() {
        return maxNumberWorkers;
    }

    public YCoVDBConfig setMaxNumberWorkers(Integer maxNumberWorkers) {
        this.maxNumberWorkers = maxNumberWorkers;
        return this;
    }

    public String getNextalignPath() {
        return nextalignPath;
    }

    public YCoVDBConfig setNextalignPath(String nextalignPath) {
        this.nextalignPath = nextalignPath;
        return this;
    }
}
