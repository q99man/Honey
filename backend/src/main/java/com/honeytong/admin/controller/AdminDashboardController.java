package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminDashboardResponse;
import com.honeytong.admin.service.AdminDashboardService;
import com.honeytong.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminDashboardService.getDashboard(adminUserId), "OK");
    }
}
