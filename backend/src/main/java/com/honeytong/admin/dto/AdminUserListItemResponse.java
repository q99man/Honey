package com.honeytong.admin.dto;

import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserStatus;
import java.time.LocalDateTime;

public record AdminUserListItemResponse(
        Long userId,
        String nickname,
        String email,
        boolean phoneVerified,
        UserStatus status,
        UserRole role,
        String languagePreference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
