package com.honeytong.policy.cache;

import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;

public record CachedPolicy(String value, PolicyValueType valueType) {

    public static CachedPolicy from(SystemPolicy policy) {
        return new CachedPolicy(policy.getPolicyValue(), policy.getValueType());
    }
}
