package com.honeytong.analytics.controller;

import com.honeytong.analytics.dto.RegionalTrendsResponse;
import com.honeytong.analytics.dto.UserAnalyticsResponse;
import com.honeytong.analytics.service.AnalyticsService;
import com.honeytong.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/me")
    public ApiResponse<UserAnalyticsResponse> getMyAnalytics(@AuthenticationPrincipal Long userId) {
        UserAnalyticsResponse response = analyticsService.getUserAnalytics(userId);
        return ApiResponse.success(response, "OK");
    }

    @GetMapping("/regions/{dongId}/trends")
    public ApiResponse<RegionalTrendsResponse> getRegionalTrends(@PathVariable Long dongId) {
        RegionalTrendsResponse response = analyticsService.getRegionalTrends(dongId);
        return ApiResponse.success(response, "OK");
    }
}
