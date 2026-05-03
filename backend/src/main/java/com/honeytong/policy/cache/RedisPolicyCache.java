package com.honeytong.policy.cache;

import com.honeytong.policy.entity.PolicyValueType;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisPolicyCache implements PolicyCache {

    private static final String KEY_PREFIX = "policy:";
    private static final String VALUE_SEPARATOR = "\n";

    private final StringRedisTemplate redisTemplate;
    private final PolicyRedisProperties properties;

    public RedisPolicyCache(StringRedisTemplate redisTemplate, PolicyRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<CachedPolicy> get(String policyGroup, String policyKey) {
        try {
            String rawValue = redisTemplate.opsForValue().get(key(policyGroup, policyKey));
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            int separator = rawValue.indexOf(VALUE_SEPARATOR);
            if (separator <= 0 || separator == rawValue.length() - 1) {
                evict(policyGroup, policyKey);
                return Optional.empty();
            }
            PolicyValueType valueType = PolicyValueType.valueOf(rawValue.substring(0, separator));
            String value = rawValue.substring(separator + VALUE_SEPARATOR.length());
            return Optional.of(new CachedPolicy(value, valueType));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String policyGroup, String policyKey, CachedPolicy policy) {
        try {
            String rawValue = policy.valueType().name() + VALUE_SEPARATOR + policy.value();
            redisTemplate.opsForValue().set(key(policyGroup, policyKey), rawValue, properties.policyCacheTtl());
        } catch (RuntimeException exception) {
            // Policy DB rows remain the source of truth when Redis is unavailable.
        }
    }

    @Override
    public void evict(String policyGroup, String policyKey) {
        try {
            redisTemplate.delete(key(policyGroup, policyKey));
        } catch (RuntimeException exception) {
            // A cache eviction failure must not break the committed policy update.
        }
    }

    private String key(String policyGroup, String policyKey) {
        return KEY_PREFIX + policyGroup + ":" + policyKey;
    }
}
