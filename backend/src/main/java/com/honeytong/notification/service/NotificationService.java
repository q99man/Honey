package com.honeytong.notification.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.notification.dto.NotificationResponse;
import com.honeytong.notification.entity.Notification;
import com.honeytong.notification.entity.NotificationType;
import com.honeytong.notification.repository.NotificationRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "알림을 찾을 수 없습니다."));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "알림을 읽을 권한이 없습니다.");
        }

        notification.markAsRead();
    }

    public void createNotification(Long userId, NotificationType type, String title, String content, Long targetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(user, type, title, content, targetId);
        notificationRepository.save(notification);
    }
}
