package com.honeytong.admin.dto;

import com.honeytong.user.entity.UserSanctionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AdminUserSanctionRequest(
        @NotNull UserSanctionType sanctionType,
        @Size(max = 255) String reason,
        LocalDateTime startAt,
        LocalDateTime endAt,
        @Size(max = 255) String memo
) {
}
