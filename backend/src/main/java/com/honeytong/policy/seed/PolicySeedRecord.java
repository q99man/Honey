package com.honeytong.policy.seed;

import com.honeytong.policy.entity.PolicyValueType;

public record PolicySeedRecord(
        String policyGroup,
        String policyKey,
        String policyValue,
        PolicyValueType valueType,
        String description
) {
}
