package ch.ethz.lapis.api.controller.v1;

import ch.ethz.lapis.api.entity.req.DataFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;


public class SampleResponseBuilder<E> {

    private String eTag;
    private Long dataVersion;
    private boolean forDownload;
    private String downloadFileName;
    private boolean allowCaching;
    private DataFormat dataFormat = DataFormat.JSON;
    private E body;

    public ResponseEntity<E> build() {
        ResponseEntity.BodyBuilder builder1 = ResponseEntity.ok();
        // ETag
        if (eTag != null) {
            builder1 = builder1.eTag(eTag);
        }
        // Headers
        HttpHeaders httpHeaders = new HttpHeaders();
        if (dataVersion != null) {
            httpHeaders.set("LAPIS-Data-Version", String.valueOf(dataVersion));
        }
        if (forDownload) {
            String fileNameWithEnding = downloadFileName + switch (dataFormat) {
                case CSV -> ".csv";
                case JSON -> ".json";
                case TEXT -> "txt";
                case FASTA -> ".fasta";
            };
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileNameWithEnding + "\"");
        }
        builder1 = builder1.headers(httpHeaders);
        // Cache control
        if (allowCaching) {
            builder1 = builder1.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
        } else {
            builder1 = builder1.cacheControl(CacheControl.noStore());
        }
        // Content-type
        MediaType mediaType = switch (dataFormat) {
            case CSV -> new MediaType("text", "csv");
            case JSON -> MediaType.APPLICATION_JSON;
            case TEXT -> MediaType.TEXT_PLAIN;
            case FASTA -> new MediaType("text", "x-fasta");
        };
        builder1 = builder1.contentType(mediaType);
        // Body
        return builder1.body(this.body);
    }

    public String getETag() {
        return eTag;
    }

    public SampleResponseBuilder<E> setETag(String eTag) {
        this.eTag = eTag;
        return this;
    }

    public Long getDataVersion() {
        return dataVersion;
    }

    public SampleResponseBuilder<E> setDataVersion(Long dataVersion) {
        this.dataVersion = dataVersion;
        return this;
    }

    public boolean isForDownload() {
        return forDownload;
    }

    public SampleResponseBuilder<E> setForDownload(boolean forDownload) {
        this.forDownload = forDownload;
        return this;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public SampleResponseBuilder<E> setDownloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
        return this;
    }

    public boolean isAllowCaching() {
        return allowCaching;
    }

    public SampleResponseBuilder<E> setAllowCaching(boolean allowCaching) {
        this.allowCaching = allowCaching;
        return this;
    }

    public DataFormat getDataFormat() {
        return dataFormat;
    }

    public SampleResponseBuilder<E> setDataFormat(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
        return this;
    }

    public E getBody() {
        return body;
    }

    public SampleResponseBuilder<E> setBody(E body) {
        this.body = body;
        return this;
    }
}
