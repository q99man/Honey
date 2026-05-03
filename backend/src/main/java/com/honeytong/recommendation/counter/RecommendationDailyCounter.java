package com.honeytong.recommendation.counter;

import java.time.LocalDate;
import java.util.function.LongSupplier;

public interface RecommendationDailyCounter {

    long getUsedCount(Long userId, LocalDate date, LongSupplier databaseCountSupplier);

    void evict(Long userId, LocalDate date);
}
