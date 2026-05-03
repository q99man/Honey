package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.entity.ReportTargetType;
import java.time.LocalDateTime;

public record AdminReportResponse(
        Long reportId,
        Long reporterUserId,
        String reporterNickname,
        ReportTargetType targetType,
        Long targetId,
        String reasonCode,
        String reasonText,
        ReportStatus status,
        Long reviewedByUserId,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
