package ch.ethz.lapis.api.entity.req;


public class GeneralConfig {

    private Long dataVersion;
    private boolean downloadAsFile = false;
    private DataFormat dataFormat = DataFormat.JSON;

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

    public DataFormat getDataFormat() {
        return dataFormat;
    }

    public GeneralConfig setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        return this;
    }
}
