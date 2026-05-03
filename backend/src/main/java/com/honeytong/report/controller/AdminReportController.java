package com.honeytong.report.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.report.dto.AdminReportFollowUpActionRequest;
import com.honeytong.report.dto.AdminReportFollowUpActionResponse;
import com.honeytong.report.dto.AdminReportProcessRequest;
import com.honeytong.report.dto.AdminReportResponse;
import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.service.AdminReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ApiResponse<List<AdminReportResponse>> getReports(
            @AuthenticationPrincipal Long adminUserId,
            @RequestParam(required = false) ReportStatus status
    ) {
        return ApiResponse.success(adminReportService.getReports(adminUserId, status), "OK");
    }

    @GetMapping("/{reportId}")
    public ApiResponse<AdminReportResponse> getReport(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.success(adminReportService.getReport(adminUserId, reportId), "OK");
    }

    @PatchMapping("/{reportId}")
    public ApiResponse<AdminReportResponse> processReport(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportProcessRequest request
    ) {
        return ApiResponse.success(adminReportService.processReport(adminUserId, reportId, request), "Report processed");
    }

    @PostMapping("/{reportId}/actions")
    public ApiResponse<AdminReportFollowUpActionResponse> applyFollowUpAction(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportFollowUpActionRequest request
    ) {
        return ApiResponse.success(
                adminReportService.applyFollowUpAction(adminUserId, reportId, request),
                "Report follow-up action applied"
        );
    }
}
