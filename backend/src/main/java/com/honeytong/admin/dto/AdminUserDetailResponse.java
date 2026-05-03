package com.honeytong.admin.dto;

import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserStatus;
import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long userId,
        String nickname,
        String email,
        String phone,
        boolean phoneVerified,
        UserStatus status,
        UserRole role,
        String languagePreference,
        boolean marketingAgreed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        AdminUserTrustResponse trust,
        AdminUserLevelResponse level
) {
}
