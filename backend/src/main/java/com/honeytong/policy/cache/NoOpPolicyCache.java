package com.honeytong.policy.cache;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPolicyCache implements PolicyCache {

    @Override
    public Optional<CachedPolicy> get(String policyGroup, String policyKey) {
        return Optional.empty();
    }

    @Override
    public void put(String policyGroup, String policyKey, CachedPolicy policy) {
    }

    @Override
    public void evict(String policyGroup, String policyKey) {
    }
}
