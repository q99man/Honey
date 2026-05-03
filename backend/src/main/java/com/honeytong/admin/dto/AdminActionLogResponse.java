package com.honeytong.admin.dto;

import java.time.LocalDateTime;

public record AdminActionLogResponse(
        Long logId,
        Long adminUserId,
        String adminNickname,
        String actionType,
        String targetType,
        Long targetId,
        String beforeValue,
        String afterValue,
        String memo,
        LocalDateTime createdAt
) {
}
