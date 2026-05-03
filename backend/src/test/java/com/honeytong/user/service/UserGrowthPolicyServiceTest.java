package com.honeytong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.TrustGrade;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGrowthPolicyServiceTest {

    @Mock
    private PolicyService policyService;

    private UserGrowthPolicyService userGrowthPolicyService;

    @BeforeEach
    void setUp() {
        userGrowthPolicyService = new UserGrowthPolicyService(policyService);
    }

    @Test
    void getNextLevelExp_readsThresholdForCurrentLevel() {
        when(policyService.getRequiredString("growth", "level_exp_thresholds"))
                .thenReturn("1:10;2:20;3:30");

        OptionalInt response = userGrowthPolicyService.getNextLevelExp(2);

        assertThat(response).hasValue(20);
    }

    @Test
    void getNextLevelExp_returnsEmptyWhenNoNextThresholdExists() {
        when(policyService.getRequiredString("growth", "level_exp_thresholds"))
                .thenReturn("1:10");

        OptionalInt response = userGrowthPolicyService.getNextLevelExp(2);

        assertThat(response).isEmpty();
    }

    @Test
    void evaluateTrust_selectsHighestEligibleGradeAndWeight() {
        when(policyService.getRequiredString("trust", "grade_thresholds"))
                .thenReturn("SEED_BEE:0;VERIFIED_BEE:5;LOCAL_BEE:10");
        when(policyService.getRequiredString("trust", "recommend_weight_by_grade"))
                .thenReturn("SEED_BEE:1.00;VERIFIED_BEE:1.05;LOCAL_BEE:1.10");

        TrustEvaluationResult response = userGrowthPolicyService.evaluateTrust(12);

        assertThat(response.trustGrade()).isEqualTo(TrustGrade.LOCAL_BEE);
        assertThat(response.recommendWeight()).isEqualByComparingTo("1.10");
    }

    @Test
    void evaluateTrust_rejectsUnknownGradePolicy() {
        when(policyService.getRequiredString("trust", "grade_thresholds"))
                .thenReturn("UNKNOWN:0");

        assertThatThrownBy(() -> userGrowthPolicyService.evaluateTrust(1))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));
    }
}
