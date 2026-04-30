package com.honeytong.policy.dto;

import com.honeytong.policy.entity.PolicyValueType;
import java.time.LocalDateTime;

public record AdminPolicyResponse(
        Long id,
        String policyGroup,
        String policyKey,
        String fullKey,
        String policyValue,
        PolicyValueType valueType,
        String description,
        boolean active,
        Long updatedBy,
        LocalDateTime updatedAt
) {
}
