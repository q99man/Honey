package com.honeytong.recommendation.counter;

import java.time.LocalDate;
import java.util.function.LongSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpRecommendationDailyCounter implements RecommendationDailyCounter {

    @Override
    public long getUsedCount(Long userId, LocalDate date, LongSupplier databaseCountSupplier) {
        return databaseCountSupplier.getAsLong();
    }

    @Override
    public void evict(Long userId, LocalDate date) {
    }
}
