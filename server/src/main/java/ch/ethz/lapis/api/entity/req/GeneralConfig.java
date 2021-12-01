package ch.ethz.lapis.api.entity.req;


public class GeneralConfig {

    private Long dataVersion;

    public Long getDataVersion() {
        return dataVersion;
    }

    public GeneralConfig setDataVersion(Long dataVersion) {
        this.dataVersion = dataVersion;
        return this;
    }
}
