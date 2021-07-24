package ch.ethz.lapis;

import ch.ethz.lapis.core.Config;
import ch.ethz.lapis.core.DatabaseConfig;

public class LapisConfig implements Config {

    private DatabaseConfig vineyard;

    private String workdir;

    private Integer maxNumberWorkers;

    private String nextalignPath;

    public DatabaseConfig getVineyard() {
        return vineyard;
    }

    public LapisConfig setVineyard(DatabaseConfig vineyard) {
        this.vineyard = vineyard;
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
}
