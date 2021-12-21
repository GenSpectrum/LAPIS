package ch.ethz.lapis.api.entity.req;


public class GeneralConfig {

    private Long dataVersion;
    private boolean downloadAsFile = false;

    public Long getDataVersion() {
        return dataVersion;
    }

    public GeneralConfig setDataVersion(Long dataVersion) {
        this.dataVersion = dataVersion;
        return this;
    }

    public boolean isDownloadAsFile() {
        return downloadAsFile;
    }

    public GeneralConfig setDownloadAsFile(boolean downloadAsFile) {
        this.downloadAsFile = downloadAsFile;
        return this;
    }
}
