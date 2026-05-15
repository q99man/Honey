package com.honeytong.report.dto;

import com.honeytong.user.entity.UserSanctionType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AdminReportFollowUpActionRequest(
        @NotNull AdminReportFollowUpActionType actionType,
        UserSanctionType sanctionType,
        String reason,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String memo
) {
}
