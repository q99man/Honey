package com.honeytong.mission.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record MissionRedisProperties(
        Duration missionCacheTtl
) {

    private static final Duration DEFAULT_MISSION_CACHE_TTL = Duration.ofMinutes(10);

    public MissionRedisProperties {
        if (missionCacheTtl == null || missionCacheTtl.isZero() || missionCacheTtl.isNegative()) {
            missionCacheTtl = DEFAULT_MISSION_CACHE_TTL;
        }
    }
}
