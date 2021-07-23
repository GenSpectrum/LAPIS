package ch.ethz.y.core;

import java.util.List;

public interface SendableReportWithAttachments extends SendableReport {

    List<ReportAttachment> getAttachments();

}
