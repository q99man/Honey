package com.honeytong.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record UserAnalyticsResponse(
        List<CategoryPreference> categoryPreferences,
        List<PricePreference> priceRangePreferences,
        List<ActivityTrend> recentActivityTrends
) {
    public record CategoryPreference(
            String categoryCode,
            double percentage
    ) {}

    public record PricePreference(
            String priceRangeCode,
            double percentage
    ) {}

    public record ActivityTrend(
            LocalDate date,
            long recommendationsCount,
            long visitsCount,
            long commentsCount
    ) {}
}
