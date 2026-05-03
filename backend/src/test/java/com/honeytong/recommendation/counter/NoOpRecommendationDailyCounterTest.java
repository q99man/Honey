package com.honeytong.recommendation.counter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NoOpRecommendationDailyCounterTest {

    @Test
    void getUsedCount_returnsDatabaseCount() {
        NoOpRecommendationDailyCounter counter = new NoOpRecommendationDailyCounter();

        long usedCount = counter.getUsedCount(1L, LocalDate.of(2026, 5, 2), () -> 3L);

        assertThat(usedCount).isEqualTo(3L);
    }
}
