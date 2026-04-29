package com.honeytong.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
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

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(systemPolicyRepository);
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

        int value = policyService.getRequiredInteger("region", "change_cooldown_day");

        assertThat(value).isEqualTo(7);
    }
}
