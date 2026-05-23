package com.honeytong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.notification.dto.NotificationResponse;
import com.honeytong.notification.entity.Notification;
import com.honeytong.notification.entity.NotificationType;
import com.honeytong.notification.repository.NotificationRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long NOTIFICATION_ID = 100L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;
    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void getNotifications_returnsListOrderedByCreatedAt() {
        Notification notification = new Notification(user, NotificationType.SYSTEM, "제목", "내용", null);
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotifications(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.get(0).title()).isEqualTo("제목");
        assertThat(result.get(0).content()).isEqualTo("내용");
        verify(notificationRepository).findAllByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void markAsRead_successfullyMarksNotificationAsRead() {
        Notification notification = new Notification(user, NotificationType.SYSTEM, "제목", "내용", null);
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(USER_ID, NOTIFICATION_ID);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_throwsForbiddenWhenNotOwner() {
        Notification notification = new Notification(user, NotificationType.SYSTEM, "제목", "내용", null);
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(OTHER_USER_ID, NOTIFICATION_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void markAsRead_throwsNotFoundWhenNotificationDoesNotExist() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, NOTIFICATION_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void createNotification_savesSuccessfully() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        notificationService.createNotification(USER_ID, NotificationType.SYSTEM, "제목", "내용", 200L);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_throwsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(USER_ID, NotificationType.SYSTEM, "제목", "내용", 200L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
