package com.honeytong.notification.dto;

import com.honeytong.notification.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String content,
        Long targetId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
