package com.honeytong.analytics.cache;

import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.dto.RegionalTrendsResponse;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class NoOpAnalyticsCache implements AnalyticsCache {

    @Override
    public Optional<RegionalTrendsResponse> getRegionalTrends(Long dongId) {
        return Optional.empty();
    }

    @Override
    public void putRegionalTrends(Long dongId, RegionalTrendsResponse trends) {
    }

    @Override
    public void evictRegionalTrends(Long dongId) {
    }

    @Override
    public Optional<AdminAnalyticsResponse> getAdminAnalytics() {
        return Optional.empty();
    }

    @Override
    public void putAdminAnalytics(AdminAnalyticsResponse trends) {
    }

    @Override
    public void evictAdminAnalytics() {
    }
}
