package com.honeytong.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.policy.cache.CachedPolicy;
import com.honeytong.policy.cache.PolicyCache;
import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private SystemPolicyRepository systemPolicyRepository;

    @Mock
    private PolicyCache policyCache;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(systemPolicyRepository, policyCache);
    }

    @Test
    void getRequiredInteger_returnsIntegerPolicyValue() {
        SystemPolicy policy = new SystemPolicy(
                "region",
                "change_cooldown_day",
                "7",
                PolicyValueType.INTEGER,
                "지역 변경 쿨다운 일수"
        );
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKeyAndActiveTrue(
                "region",
                "change_cooldown_day"
        )).thenReturn(Optional.of(policy));
        when(policyCache.get("region", "change_cooldown_day")).thenReturn(Optional.empty());

        int value = policyService.getRequiredInteger("region", "change_cooldown_day");

        assertThat(value).isEqualTo(7);
        verify(policyCache).put("region", "change_cooldown_day", CachedPolicy.from(policy));
    }

    @Test
    void getRequiredDecimal_returnsDecimalPolicyValue() {
        SystemPolicy policy = new SystemPolicy(
                "ranking",
                "visit_weight",
                "2.0",
                PolicyValueType.DECIMAL,
                "방문 점수 가중치"
        );
        when(systemPolicyRepository.findByPolicyGroupAndPolicyKeyAndActiveTrue(
                "ranking",
                "visit_weight"
        )).thenReturn(Optional.of(policy));
        when(policyCache.get("ranking", "visit_weight")).thenReturn(Optional.empty());

        BigDecimal value = policyService.getRequiredDecimal("ranking", "visit_weight");

        assertThat(value).isEqualByComparingTo("2.0");
    }

    @Test
    void getRequiredString_returnsCachedValueWithoutRepositoryLookup() {
        when(policyCache.get("region", "registration_scope"))
                .thenReturn(Optional.of(new CachedPolicy("CITY", PolicyValueType.STRING)));

        String value = policyService.getRequiredString("region", "registration_scope");

        assertThat(value).isEqualTo("CITY");
        verify(systemPolicyRepository, never()).findByPolicyGroupAndPolicyKeyAndActiveTrue(any(), any());
    }
}
