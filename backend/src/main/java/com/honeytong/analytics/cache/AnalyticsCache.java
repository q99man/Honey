package com.honeytong.analytics.cache;

import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.dto.RegionalTrendsResponse;
import java.util.Optional;

public interface AnalyticsCache {
    Optional<RegionalTrendsResponse> getRegionalTrends(Long dongId);
    void putRegionalTrends(Long dongId, RegionalTrendsResponse trends);
    void evictRegionalTrends(Long dongId);

    Optional<AdminAnalyticsResponse> getAdminAnalytics();
    void putAdminAnalytics(AdminAnalyticsResponse trends);
    void evictAdminAnalytics();
}
