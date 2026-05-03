package com.honeytong.report.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.report.dto.MyReportResponse;
import com.honeytong.report.dto.ReportCreateRequest;
import com.honeytong.report.dto.ReportCreateResponse;
import com.honeytong.report.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/reports")
    public ApiResponse<ReportCreateResponse> createReport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return ApiResponse.success(reportService.createReport(userId, request), "Report submitted");
    }

    @GetMapping("/users/me/reports")
    public ApiResponse<List<MyReportResponse>> getMyReports(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(reportService.getMyReports(userId), "OK");
    }
}
