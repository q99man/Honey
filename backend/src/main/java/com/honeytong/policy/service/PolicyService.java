package com.honeytong.policy.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final SystemPolicyRepository systemPolicyRepository;

    public PolicyService(SystemPolicyRepository systemPolicyRepository) {
        this.systemPolicyRepository = systemPolicyRepository;
    }

    @Transactional(readOnly = true)
    public int getRequiredInteger(String policyGroup, String policyKey) {
        SystemPolicy policy = systemPolicyRepository
                .findByPolicyGroupAndPolicyKeyAndActiveTrue(policyGroup, policyKey)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.POLICY_VIOLATION,
                        "필수 정책이 설정되어 있지 않습니다: " + policyGroup + "." + policyKey
                ));
        if (policy.getValueType() != PolicyValueType.INTEGER) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "정책 값 형식이 올바르지 않습니다: " + policyGroup + "." + policyKey
            );
        }
        try {
            return Integer.parseInt(policy.getPolicyValue());
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "정책 값을 숫자로 변환할 수 없습니다: " + policyGroup + "." + policyKey
            );
        }
    }
}
