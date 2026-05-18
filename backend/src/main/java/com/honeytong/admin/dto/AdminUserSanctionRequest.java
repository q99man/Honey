package com.honeytong.admin.dto;

import com.honeytong.user.entity.UserSanctionType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AdminUserSanctionRequest(
        @NotNull UserSanctionType sanctionType,
        String reason,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String memo
) {
}
