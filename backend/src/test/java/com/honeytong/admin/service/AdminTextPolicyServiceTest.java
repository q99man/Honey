package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTextPolicyServiceTest {

    @Mock
    private PolicyService policyService;

    private AdminTextPolicyService adminTextPolicyService;

    @BeforeEach
    void setUp() {
        adminTextPolicyService = new AdminTextPolicyService(policyService);
    }

    @Test
    void normalizeActionMemo_rejectsMemoLongerThanPolicy() {
        when(policyService.getRequiredInteger("admin", "action_memo_max_length")).thenReturn(5);

        assertThatThrownBy(() -> adminTextPolicyService.normalizeActionMemo("123456"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void normalizeSanctionReason_trimsBlankToNullAndAcceptsPolicyLength() {
        when(policyService.getRequiredInteger("admin", "sanction_reason_max_length")).thenReturn(5);

        assertThat(adminTextPolicyService.normalizeSanctionReason(" 12345 ")).isEqualTo("12345");
        assertThat(adminTextPolicyService.normalizeSanctionReason("  ")).isNull();
    }

    @Test
    void normalizeSanctionReason_rejectsInvalidPolicyAboveColumnLimit() {
        when(policyService.getRequiredInteger("admin", "sanction_reason_max_length")).thenReturn(256);

        assertThatThrownBy(() -> adminTextPolicyService.normalizeSanctionReason("reason"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));
    }
}
