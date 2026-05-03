package com.honeytong.ranking.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RankingSchedulerPropertiesTest {

    @Test
    void constructorAppliesSafeDefaults() {
        RankingSchedulerProperties properties = new RankingSchedulerProperties(false, null, " ", "");

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.cron()).isEqualTo(RankingSchedulerProperties.DEFAULT_CRON);
        assertThat(properties.zone()).isEqualTo(RankingSchedulerProperties.DEFAULT_ZONE);
        assertThat(properties.seasonCode()).isNull();
    }
}
