package ch.ethz.lapis.core;

public record ReportAttachment(String attachmentFilename, byte[] bytes, String mimeType) {
}
