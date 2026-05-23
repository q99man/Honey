package com.honeytong.ranking.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record SeasonRedisProperties(
        Duration seasonCacheTtl
) {

    private static final Duration DEFAULT_SEASON_CACHE_TTL = Duration.ofHours(1);

    public SeasonRedisProperties {
        if (seasonCacheTtl == null || seasonCacheTtl.isZero() || seasonCacheTtl.isNegative()) {
            seasonCacheTtl = DEFAULT_SEASON_CACHE_TTL;
        }
    }
}
