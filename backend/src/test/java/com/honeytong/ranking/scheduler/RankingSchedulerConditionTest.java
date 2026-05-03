package com.honeytong.ranking.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.honeytong.ranking.service.RankingRecalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class RankingSchedulerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void schedulerBeanIsDisabledByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(ScheduledRankingRecalculationJob.class));
    }

    @Test
    void schedulerBeanIsCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues("app.ranking.scheduler.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(ScheduledRankingRecalculationJob.class));
    }

    @Configuration
    @Import(ScheduledRankingRecalculationJob.class)
    @EnableConfigurationProperties(RankingSchedulerProperties.class)
    static class TestConfig {

        @Bean
        RankingRecalculationService rankingRecalculationService() {
            return mock(RankingRecalculationService.class);
        }
    }
}
