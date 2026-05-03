package com.honeytong.report.dto;

import com.honeytong.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull ReportTargetType targetType,
        @NotNull @Positive Long targetId,
        @NotBlank @Size(max = 50) String reasonCode,
        @Size(max = 255) String reasonText
) {
}
