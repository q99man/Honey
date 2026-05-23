package com.honeytong.analytics.controller;

import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.service.AdminAnalyticsService;
import com.honeytong.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/activity-trends")
    public ApiResponse<AdminAnalyticsResponse> getGlobalActivityTrends(@AuthenticationPrincipal Long adminUserId) {
        AdminAnalyticsResponse response = adminAnalyticsService.getGlobalActivityTrends(adminUserId);
        return ApiResponse.success(response, "OK");
    }
}
