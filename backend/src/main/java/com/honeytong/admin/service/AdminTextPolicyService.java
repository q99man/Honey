package com.honeytong.admin.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import org.springframework.stereotype.Service;

@Service
public class AdminTextPolicyService {

    private static final String ADMIN_POLICY_GROUP = "admin";
    private static final String SANCTION_REASON_MAX_LENGTH_KEY = "sanction_reason_max_length";
    private static final String ACTION_MEMO_MAX_LENGTH_KEY = "action_memo_max_length";
    private static final int SANCTION_REASON_COLUMN_LIMIT = 255;
    private static final int ACTION_MEMO_COLUMN_LIMIT = 255;

    private final PolicyService policyService;

    public AdminTextPolicyService(PolicyService policyService) {
        this.policyService = policyService;
    }

    public String normalizeSanctionReason(String value) {
        return normalizeAndValidate(
                value,
                SANCTION_REASON_MAX_LENGTH_KEY,
                SANCTION_REASON_COLUMN_LIMIT,
                "제재 사유"
        );
    }

    public String normalizeActionMemo(String value) {
        return normalizeAndValidate(
                value,
                ACTION_MEMO_MAX_LENGTH_KEY,
                ACTION_MEMO_COLUMN_LIMIT,
                "관리자 작업 메모"
        );
    }

    private String normalizeAndValidate(String value, String policyKey, int columnLimit, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int maxLength = policyService.getRequiredInteger(ADMIN_POLICY_GROUP, policyKey);
        if (maxLength <= 0 || maxLength > columnLimit) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    label + " 길이 정책은 1-" + columnLimit + " 사이여야 합니다."
            );
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, label + " 길이가 정책 허용치를 초과했습니다.");
        }
        return normalized;
    }
}
