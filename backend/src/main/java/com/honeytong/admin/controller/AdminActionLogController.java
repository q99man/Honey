package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminActionLogResponse;
import com.honeytong.admin.service.AdminActionLogService;
import com.honeytong.common.api.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/action-logs")
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    public AdminActionLogController(AdminActionLogService adminActionLogService) {
        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping
    public ApiResponse<List<AdminActionLogResponse>> getActionLogs(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminActionLogService.getActionLogs(adminUserId), "OK");
    }
}
