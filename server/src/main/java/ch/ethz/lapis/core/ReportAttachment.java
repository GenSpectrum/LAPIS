package ch.ethz.lapis.core;

public class ReportAttachment {
    private final String attachmentFilename;
    private final byte[] bytes;
    private final String mimeType;

    public ReportAttachment(String attachmentFilename, byte[] bytes, String mimeType) {
        this.attachmentFilename = attachmentFilename;
        this.bytes = bytes;
        this.mimeType = mimeType;
    }

    public String getAttachmentFilename() {
        return attachmentFilename;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getMimeType() {
        return mimeType;
    }
}
