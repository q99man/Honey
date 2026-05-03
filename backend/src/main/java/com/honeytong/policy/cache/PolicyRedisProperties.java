package com.honeytong.policy.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record PolicyRedisProperties(
        boolean enabled,
        Duration policyCacheTtl
) {

    private static final Duration DEFAULT_POLICY_CACHE_TTL = Duration.ofMinutes(10);

    public PolicyRedisProperties {
        if (policyCacheTtl == null || policyCacheTtl.isZero() || policyCacheTtl.isNegative()) {
            policyCacheTtl = DEFAULT_POLICY_CACHE_TTL;
        }
    }
}
