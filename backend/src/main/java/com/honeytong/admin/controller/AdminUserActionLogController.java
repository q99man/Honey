package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminUserActionLogResponse;
import com.honeytong.admin.service.AdminUserActionLogService;
import com.honeytong.common.api.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user-action-logs")
public class AdminUserActionLogController {

    private final AdminUserActionLogService adminUserActionLogService;

    public AdminUserActionLogController(AdminUserActionLogService adminUserActionLogService) {
        this.adminUserActionLogService = adminUserActionLogService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserActionLogResponse>> getUserActionLogs(
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ApiResponse.success(adminUserActionLogService.getUserActionLogs(adminUserId), "OK");
    }
}
