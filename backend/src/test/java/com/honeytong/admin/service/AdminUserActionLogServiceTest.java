package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserActionLog;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserActionLogRepository;
import com.honeytong.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserActionLogServiceTest {

    private static final long ADMIN_ID = 1L;

    @Mock
    private UserActionLogRepository userActionLogRepository;

    @Mock
    private UserRepository userRepository;

    private AdminUserActionLogService adminUserActionLogService;
    private User admin;

    @BeforeEach
    void setUp() {
        adminUserActionLogService = new AdminUserActionLogService(userActionLogRepository, userRepository);

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
    }

    @Test
    void getUserActionLogs_returnsLatestUserActionLogs() {
        User user = new User("bee", "bee@example.com");
        ReflectionTestUtils.setField(user, "id", 9L);
        UserActionLog log = new UserActionLog(
                user,
                "VISIT_VERIFY",
                "PLACE",
                100L,
                "127.0.0.1",
                "HoneytongApp/1.0",
                "{\"distanceMeter\":42}"
        );
        ReflectionTestUtils.setField(log, "id", 700L);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 5, 2, 11, 20));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userActionLogRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        var response = adminUserActionLogService.getUserActionLogs(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().logId()).isEqualTo(700L);
        assertThat(response.getFirst().userId()).isEqualTo(9L);
        assertThat(response.getFirst().nickname()).isEqualTo("bee");
        assertThat(response.getFirst().actionType()).isEqualTo("VISIT_VERIFY");
        assertThat(response.getFirst().targetType()).isEqualTo("PLACE");
        assertThat(response.getFirst().targetId()).isEqualTo(100L);
        assertThat(response.getFirst().ipAddress()).isEqualTo("127.0.0.1");
        assertThat(response.getFirst().userAgent()).isEqualTo("HoneytongApp/1.0");
        assertThat(response.getFirst().metadataJson()).isEqualTo("{\"distanceMeter\":42}");
        assertThat(response.getFirst().createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 11, 20));
    }

    @Test
    void getUserActionLogs_allowsSuperAdmin() {
        ReflectionTestUtils.setField(admin, "role", UserRole.SUPER_ADMIN);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        var response = adminUserActionLogService.getUserActionLogs(ADMIN_ID);

        assertThat(response).isEmpty();
    }

    @Test
    void getUserActionLogs_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminUserActionLogService.getUserActionLogs(9L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
