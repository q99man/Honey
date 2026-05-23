package com.honeytong.analytics.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record AnalyticsRedisProperties(
        Duration regionalTrendsTtl,
        Duration adminTrendsTtl
) {
    private static final Duration DEFAULT_REGIONAL_TRENDS_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_ADMIN_TRENDS_TTL = Duration.ofHours(2);

    public AnalyticsRedisProperties {
        if (regionalTrendsTtl == null || regionalTrendsTtl.isZero() || regionalTrendsTtl.isNegative()) {
            regionalTrendsTtl = DEFAULT_REGIONAL_TRENDS_TTL;
        }
        if (adminTrendsTtl == null || adminTrendsTtl.isZero() || adminTrendsTtl.isNegative()) {
            adminTrendsTtl = DEFAULT_ADMIN_TRENDS_TTL;
        }
    }
}
