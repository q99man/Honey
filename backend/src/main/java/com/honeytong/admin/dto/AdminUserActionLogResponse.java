package com.honeytong.admin.dto;

import java.time.LocalDateTime;

public record AdminUserActionLogResponse(
        Long logId,
        Long userId,
        String nickname,
        String actionType,
        String targetType,
        Long targetId,
        String ipAddress,
        String userAgent,
        String metadataJson,
        LocalDateTime createdAt
) {
}
