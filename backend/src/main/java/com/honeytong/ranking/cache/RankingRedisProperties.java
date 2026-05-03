package com.honeytong.ranking.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record RankingRedisProperties(
        Duration rankingCacheTtl
) {

    private static final Duration DEFAULT_RANKING_CACHE_TTL = Duration.ofMinutes(5);

    public RankingRedisProperties {
        if (rankingCacheTtl == null || rankingCacheTtl.isZero() || rankingCacheTtl.isNegative()) {
            rankingCacheTtl = DEFAULT_RANKING_CACHE_TTL;
        }
    }
}
