package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
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
class AdminActionLogServiceTest {

    private static final long ADMIN_ID = 1L;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private UserRepository userRepository;

    private AdminActionLogService adminActionLogService;
    private User admin;

    @BeforeEach
    void setUp() {
        adminActionLogService = new AdminActionLogService(adminActionLogRepository, userRepository);

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
    }

    @Test
    void getActionLogs_returnsLatestAuditLogs() {
        AdminActionLog log = new AdminActionLog(
                admin,
                "COMMENT_BLIND",
                "COMMENT",
                500L,
                "{\"status\":\"VISIBLE\"}",
                "{\"status\":\"BLINDED\"}",
                "Hide after review."
        );
        ReflectionTestUtils.setField(log, "id", 900L);
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 5, 2, 10, 30));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(adminActionLogRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(log));

        var response = adminActionLogService.getActionLogs(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().logId()).isEqualTo(900L);
        assertThat(response.getFirst().adminUserId()).isEqualTo(ADMIN_ID);
        assertThat(response.getFirst().adminNickname()).isEqualTo("admin");
        assertThat(response.getFirst().actionType()).isEqualTo("COMMENT_BLIND");
        assertThat(response.getFirst().targetType()).isEqualTo("COMMENT");
        assertThat(response.getFirst().targetId()).isEqualTo(500L);
        assertThat(response.getFirst().beforeValue()).isEqualTo("{\"status\":\"VISIBLE\"}");
        assertThat(response.getFirst().afterValue()).isEqualTo("{\"status\":\"BLINDED\"}");
        assertThat(response.getFirst().memo()).isEqualTo("Hide after review.");
        assertThat(response.getFirst().createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 10, 30));
    }

    @Test
    void getActionLogs_allowsSuperAdmin() {
        ReflectionTestUtils.setField(admin, "role", UserRole.SUPER_ADMIN);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        var response = adminActionLogService.getActionLogs(ADMIN_ID);

        assertThat(response).isEmpty();
    }

    @Test
    void getActionLogs_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminActionLogService.getActionLogs(9L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
