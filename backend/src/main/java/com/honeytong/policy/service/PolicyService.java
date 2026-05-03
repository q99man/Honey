package com.honeytong.policy.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.cache.CachedPolicy;
import com.honeytong.policy.cache.PolicyCache;
import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final SystemPolicyRepository systemPolicyRepository;
    private final PolicyCache policyCache;

    public PolicyService(SystemPolicyRepository systemPolicyRepository, PolicyCache policyCache) {
        this.systemPolicyRepository = systemPolicyRepository;
        this.policyCache = policyCache;
    }

    @Transactional(readOnly = true)
    public int getRequiredInteger(String policyGroup, String policyKey) {
        CachedPolicy policy = getRequiredPolicy(policyGroup, policyKey);
        if (policy.valueType() != PolicyValueType.INTEGER) {
            throw invalidType(policyGroup, policyKey);
        }
        try {
            return Integer.parseInt(policy.value());
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "정책 값을 정수로 변환할 수 없습니다: " + fullKey(policyGroup, policyKey)
            );
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getRequiredDecimal(String policyGroup, String policyKey) {
        CachedPolicy policy = getRequiredPolicy(policyGroup, policyKey);
        if (policy.valueType() != PolicyValueType.DECIMAL) {
            throw invalidType(policyGroup, policyKey);
        }
        try {
            return new BigDecimal(policy.value());
        } catch (NumberFormatException exception) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "정책 값을 숫자로 변환할 수 없습니다: " + fullKey(policyGroup, policyKey)
            );
        }
    }

    @Transactional(readOnly = true)
    public String getRequiredString(String policyGroup, String policyKey) {
        CachedPolicy policy = getRequiredPolicy(policyGroup, policyKey);
        if (policy.valueType() != PolicyValueType.STRING) {
            throw invalidType(policyGroup, policyKey);
        }
        return policy.value();
    }

    private CachedPolicy getRequiredPolicy(String policyGroup, String policyKey) {
        return policyCache.get(policyGroup, policyKey)
                .orElseGet(() -> loadRequiredPolicy(policyGroup, policyKey));
    }

    private CachedPolicy loadRequiredPolicy(String policyGroup, String policyKey) {
        SystemPolicy policy = systemPolicyRepository
                .findByPolicyGroupAndPolicyKeyAndActiveTrue(policyGroup, policyKey)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.POLICY_VIOLATION,
                        "필수 정책이 설정되어 있지 않습니다: " + fullKey(policyGroup, policyKey)
                ));
        CachedPolicy cachedPolicy = CachedPolicy.from(policy);
        policyCache.put(policyGroup, policyKey, cachedPolicy);
        return cachedPolicy;
    }

    private ApiException invalidType(String policyGroup, String policyKey) {
        return new ApiException(
                ErrorCode.POLICY_VIOLATION,
                "정책 값 형식이 올바르지 않습니다: " + fullKey(policyGroup, policyKey)
        );
    }

    private String fullKey(String policyGroup, String policyKey) {
        return policyGroup + "." + policyKey;
    }
}
