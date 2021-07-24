package ch.ethz.lapis.core;

import java.util.List;

public interface SendableReportWithAttachments extends SendableReport {

    List<ReportAttachment> getAttachments();

}
