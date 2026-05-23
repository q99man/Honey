package com.honeytong.report.event;

public record ReportProcessedEvent(
        Long reporterId,
        Long reportId,
        String targetType,
        String reportStatus,
        String reviewNote
) {}
