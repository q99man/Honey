package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportTargetType;

public record AdminReportFollowUpActionResponse(
        Long reportId,
        AdminReportFollowUpActionType actionType,
        ReportTargetType reportTargetType,
        Long reportTargetId,
        String resultTargetType,
        Long resultTargetId,
        boolean applied
) {
}
