package com.honeytong.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.policy.cache.PolicyCache;
import com.honeytong.policy.dto.AdminPolicyUpdateRequest;
import com.honeytong.policy.dto.AdminRegionPolicyRequest;
import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminPolicyServiceTest {

    private static final long ADMIN_ID = 1L;

    @Mock
    private SystemPolicyRepository systemPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private PolicyCache policyCache;

    private AdminPolicyService adminPolicyService;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        superAdmin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(superAdmin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(superAdmin, "role", UserRole.SUPER_ADMIN);

        adminPolicyService = new AdminPolicyService(
                systemPolicyRepository,
                userRepository,
                adminActionLogRepository,
                policyService,
                policyCache,
                new ObjectMapper()
        );
    }

    @Test
    void updatePolicy_validatesTypeUpdatesValueAndLogsAction() {
        SystemPolicy policy = new SystemPolicy(
                "visit",
                "radius_meter",
                "70",
                PolicyValueType.INTEGER,
                "방문 인증 허용 반경 미터"
        );
        ReflectionTestUtils.setField(policy, "id", 10L);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKey("visit", "radius_meter"))
                .thenReturn(Optional.of(policy));

        var response = adminPolicyService.updatePolicy(
                ADMIN_ID,
                "visit.radius_meter",
                new AdminPolicyUpdateRequest("80", "반경 조정")
        );

        assertThat(response.policyValue()).isEqualTo("80");
        assertThat(response.fullKey()).isEqualTo("visit.radius_meter");
        assertThat(policy.getUpdatedBy()).isEqualTo(superAdmin);
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
        verify(policyCache).evict("visit", "radius_meter");
    }

    @Test
    void updatePolicy_rejectsInvalidIntegerValue() {
        SystemPolicy policy = new SystemPolicy(
                "visit",
                "radius_meter",
                "70",
                PolicyValueType.INTEGER,
                "방문 인증 허용 반경 미터"
        );
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKey("visit", "radius_meter"))
                .thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> adminPolicyService.updatePolicy(
                ADMIN_ID,
                "visit.radius_meter",
                new AdminPolicyUpdateRequest("wide", null)
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void updateRegionPolicy_updatesBothRegionPolicies() {
        SystemPolicy cooldownPolicy = new SystemPolicy(
                "region",
                "change_cooldown_day",
                "7",
                PolicyValueType.INTEGER,
                "지역 변경 쿨다운 일수"
        );
        ReflectionTestUtils.setField(cooldownPolicy, "id", 11L);
        SystemPolicy scopePolicy = new SystemPolicy(
                "region",
                "registration_scope",
                "DONG",
                PolicyValueType.STRING,
                "장소 등록 허용 지역 범위"
        );
        ReflectionTestUtils.setField(scopePolicy, "id", 12L);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKey("region", "change_cooldown_day"))
                .thenReturn(Optional.of(cooldownPolicy));
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKey("region", "registration_scope"))
                .thenReturn(Optional.of(scopePolicy));
        when(policyService.getRequiredInteger("region", "change_cooldown_day")).thenReturn(3);
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("CITY");

        var response = adminPolicyService.updateRegionPolicy(
                ADMIN_ID,
                new AdminRegionPolicyRequest(3, "city")
        );

        assertThat(response.regionChangeCooldownDays()).isEqualTo(3);
        assertThat(response.registrationScope()).isEqualTo("CITY");
        assertThat(cooldownPolicy.getPolicyValue()).isEqualTo("3");
        assertThat(scopePolicy.getPolicyValue()).isEqualTo("CITY");
        verify(adminActionLogRepository, times(2)).save(any(AdminActionLog.class));
        verify(policyCache).evict("region", "change_cooldown_day");
        verify(policyCache).evict("region", "registration_scope");
    }
}
