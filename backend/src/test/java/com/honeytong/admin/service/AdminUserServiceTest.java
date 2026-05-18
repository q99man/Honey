package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.dto.AdminUserRecommendWeightRequest;
import com.honeytong.admin.dto.AdminUserSanctionRequest;
import com.honeytong.admin.dto.AdminUserTrustAdjustRequest;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.TrustGrade;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserSanction;
import com.honeytong.user.entity.UserSanctionType;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserSanctionRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long USER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PolicyService policyService;

    private AdminUserService adminUserService;
    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository,
                userTrustRepository,
                userLevelRepository,
                userSanctionRepository,
                adminActionLogRepository,
                new ObjectMapper(),
                new AdminTextPolicyService(policyService)
        );

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        user = new User("bee", "bee@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        stubAdminTextPolicies(255, 255);
    }

    @Test
    void getUsers_returnsUserList() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));

        var response = adminUserService.getUsers(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).userId()).isEqualTo(USER_ID);
        assertThat(response.get(0).nickname()).isEqualTo("bee");
        assertThat(response.get(0).role()).isEqualTo(UserRole.USER);
    }

    @Test
    void getUser_returnsDetailWithTrustAndLevel() {
        UserTrust trust = new UserTrust(user);
        UserLevel level = new UserLevel(user);
        ReflectionTestUtils.setField(trust, "trustScore", 12);
        ReflectionTestUtils.setField(level, "level", 3);
        ReflectionTestUtils.setField(level, "totalExp", 80);
        ReflectionTestUtils.setField(level, "rankScore", BigDecimal.valueOf(10.50));

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.of(level));

        var response = adminUserService.getUser(ADMIN_ID, USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.trust()).isNotNull();
        assertThat(response.trust().trustScore()).isEqualTo(12);
        assertThat(response.level()).isNotNull();
        assertThat(response.level().level()).isEqualTo(3);
        assertThat(response.level().totalExp()).isEqualTo(80);
    }

    @Test
    void getUser_returnsNullTrustAndLevelWhenMissing() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.empty());

        var response = adminUserService.getUser(ADMIN_ID, USER_ID);

        assertThat(response.trust()).isNull();
        assertThat(response.level()).isNull();
    }

    @Test
    void getUser_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminUserService.getUser(9L, USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getUser_returnsNotFoundForMissingUser() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUser(ADMIN_ID, USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void createSanction_savesSanctionIncrementsTrustAndLogsAction() {
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 1, 10, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 8, 10, 0);
        UserTrust trust = new UserTrust(user);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userSanctionRepository.save(org.mockito.Mockito.any(UserSanction.class)))
                .thenAnswer(invocation -> {
                    UserSanction sanction = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sanction, "id", 500L);
                    return sanction;
                });

        var response = adminUserService.createSanction(
                ADMIN_ID,
                USER_ID,
                new AdminUserSanctionRequest(
                        UserSanctionType.TEMPORARY_RESTRICTION,
                        "Spam reports confirmed.",
                        startAt,
                        endAt,
                        "Operator memo."
                )
        );

        assertThat(response.sanctionId()).isEqualTo(500L);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.sanctionType()).isEqualTo(UserSanctionType.TEMPORARY_RESTRICTION);
        assertThat(response.startAt()).isEqualTo(startAt);
        assertThat(response.endAt()).isEqualTo(endAt);
        assertThat(trust.getSanctionCount()).isEqualTo(1);

        ArgumentCaptor<AdminActionLog> logCaptor = ArgumentCaptor.forClass(AdminActionLog.class);
        org.mockito.Mockito.verify(adminActionLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue()).isNotNull();
    }

    @Test
    void createSanction_rejectsSelfSanction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserService.createSanction(
                ADMIN_ID,
                ADMIN_ID,
                new AdminUserSanctionRequest(UserSanctionType.WARNING, "Self sanction.", null, null, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createSanction_rejectsInvalidPeriod() {
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 1, 10, 0);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.createSanction(
                ADMIN_ID,
                USER_ID,
                new AdminUserSanctionRequest(UserSanctionType.WARNING, "Invalid period.", startAt, startAt, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createSanction_rejectsReasonLongerThanPolicy() {
        when(policyService.getRequiredInteger("admin", "sanction_reason_max_length")).thenReturn(5);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.createSanction(
                ADMIN_ID,
                USER_ID,
                new AdminUserSanctionRequest(UserSanctionType.WARNING, "123456", null, null, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void adjustTrust_updatesTrustAndLogsAction() {
        UserTrust trust = new UserTrust(user);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));

        var response = adminUserService.adjustTrust(
                ADMIN_ID,
                USER_ID,
                new AdminUserTrustAdjustRequest(20, TrustGrade.LOCAL_BEE, "Manual review.")
        );

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.trustScore()).isEqualTo(20);
        assertThat(response.trustGrade()).isEqualTo(TrustGrade.LOCAL_BEE);
        assertThat(response.lastEvaluatedAt()).isNotNull();
        assertThat(trust.getTrustScore()).isEqualTo(20);
        assertThat(trust.getTrustGrade()).isEqualTo(TrustGrade.LOCAL_BEE);
        org.mockito.Mockito.verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void adjustRecommendWeight_updatesWeightAndLogsAction() {
        UserTrust trust = new UserTrust(user);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));

        var response = adminUserService.adjustRecommendWeight(
                ADMIN_ID,
                USER_ID,
                new AdminUserRecommendWeightRequest(BigDecimal.valueOf(1.25), "Weight adjustment.")
        );

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.recommendWeight()).isEqualByComparingTo("1.25");
        assertThat(trust.getRecommendWeight()).isEqualByComparingTo("1.25");
        assertThat(response.lastEvaluatedAt()).isNotNull();
        org.mockito.Mockito.verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void adjustRecommendWeight_rejectsTooManyDecimalPlaces() {
        UserTrust trust = new UserTrust(user);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));

        assertThatThrownBy(() -> adminUserService.adjustRecommendWeight(
                ADMIN_ID,
                USER_ID,
                new AdminUserRecommendWeightRequest(new BigDecimal("1.234"), "Invalid scale.")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void adjustTrust_rejectsMemoLongerThanPolicy() {
        UserTrust trust = new UserTrust(user);
        when(policyService.getRequiredInteger("admin", "action_memo_max_length")).thenReturn(5);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));

        assertThatThrownBy(() -> adminUserService.adjustTrust(
                ADMIN_ID,
                USER_ID,
                new AdminUserTrustAdjustRequest(20, TrustGrade.LOCAL_BEE, "123456")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void adjustTrust_rejectsAdminAccountTarget() {
        User targetAdmin = new User("target-admin", "target-admin@example.com");
        ReflectionTestUtils.setField(targetAdmin, "id", 9L);
        ReflectionTestUtils.setField(targetAdmin, "role", UserRole.ADMIN);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(9L)).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() -> adminUserService.adjustTrust(
                ADMIN_ID,
                9L,
                new AdminUserTrustAdjustRequest(10, TrustGrade.VERIFIED_BEE, "Invalid target.")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private void stubAdminTextPolicies(int sanctionReasonMaxLength, int actionMemoMaxLength) {
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredInteger("admin", "sanction_reason_max_length"))
                .thenReturn(sanctionReasonMaxLength);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredInteger("admin", "action_memo_max_length"))
                .thenReturn(actionMemoMaxLength);
    }
}
