package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReportProcessRequest(
        @NotNull ReportStatus status,
        @Size(max = 255) String reviewNote
) {
}
