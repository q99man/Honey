package com.honeytong.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminAnalyticsResponse(
        List<DailyActivityTrend> dailyTrends
) {
    public record DailyActivityTrend(
            LocalDate date,
            long newUsersCount,
            long newPlacesCount,
            long recommendationsCount,
            long visitsCount
    ) {}
}
