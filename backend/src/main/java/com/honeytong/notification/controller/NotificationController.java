package com.honeytong.notification.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.notification.dto.NotificationResponse;
import com.honeytong.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal Long userId) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ApiResponse.success(notifications, "OK");
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.success(null, "Notification marked as read");
    }
}
