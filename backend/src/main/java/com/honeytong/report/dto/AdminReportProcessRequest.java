package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportProcessRequest(
        @NotNull ReportStatus status,
        String reviewNote
) {
}
