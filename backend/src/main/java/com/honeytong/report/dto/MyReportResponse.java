package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.entity.ReportTargetType;
import java.time.LocalDateTime;

public record MyReportResponse(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        String reasonCode,
        String reasonText,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewNote
) {
}
