package com.honeytong.admin.dto;

import com.honeytong.user.entity.UserSanctionStatus;
import com.honeytong.user.entity.UserSanctionType;
import java.time.LocalDateTime;

public record AdminUserSanctionResponse(
        Long sanctionId,
        Long userId,
        String nickname,
        UserSanctionType sanctionType,
        String reason,
        LocalDateTime startAt,
        LocalDateTime endAt,
        UserSanctionStatus status,
        Long createdByUserId,
        String createdByNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
