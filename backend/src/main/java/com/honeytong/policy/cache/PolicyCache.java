package com.honeytong.policy.cache;

import java.util.Optional;

public interface PolicyCache {

    Optional<CachedPolicy> get(String policyGroup, String policyKey);

    void put(String policyGroup, String policyKey, CachedPolicy policy);

    void evict(String policyGroup, String policyKey);
}
