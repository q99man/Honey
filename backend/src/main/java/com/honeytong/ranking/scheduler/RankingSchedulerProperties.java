package com.honeytong.ranking.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ranking.scheduler")
public record RankingSchedulerProperties(
        boolean enabled,
        String cron,
        String zone,
        String seasonCode
) {

    public static final String DEFAULT_CRON = "0 0 4 * * *";
    public static final String DEFAULT_ZONE = "Asia/Seoul";

    public RankingSchedulerProperties {
        if (cron == null || cron.isBlank()) {
            cron = DEFAULT_CRON;
        }
        if (zone == null || zone.isBlank()) {
            zone = DEFAULT_ZONE;
        }
        if (seasonCode != null && seasonCode.isBlank()) {
            seasonCode = null;
        }
    }
}
