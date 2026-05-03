package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminActivityInvalidationRequest;
import com.honeytong.admin.dto.AdminRecommendationInvalidationResponse;
import com.honeytong.admin.dto.AdminRecommendationResponse;
import com.honeytong.admin.dto.AdminVisitInvalidationResponse;
import com.honeytong.admin.dto.AdminVisitResponse;
import com.honeytong.admin.service.AdminActivityService;
import com.honeytong.common.api.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    public AdminActivityController(AdminActivityService adminActivityService) {
        this.adminActivityService = adminActivityService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<AdminRecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ApiResponse.success(adminActivityService.getRecommendations(adminUserId), "OK");
    }

    @GetMapping("/visits")
    public ApiResponse<List<AdminVisitResponse>> getVisits(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminActivityService.getVisits(adminUserId), "OK");
    }

    @PatchMapping("/recommendations/{recommendationId}/invalidate")
    public ApiResponse<AdminRecommendationInvalidationResponse> invalidateRecommendation(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long recommendationId,
            @RequestBody(required = false) AdminActivityInvalidationRequest request
    ) {
        return ApiResponse.success(
                adminActivityService.invalidateRecommendation(adminUserId, recommendationId, request),
                "Recommendation invalidated"
        );
    }

    @PatchMapping("/visits/{visitId}/invalidate")
    public ApiResponse<AdminVisitInvalidationResponse> invalidateVisit(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long visitId,
            @RequestBody(required = false) AdminActivityInvalidationRequest request
    ) {
        return ApiResponse.success(
                adminActivityService.invalidateVisit(adminUserId, visitId, request),
                "Visit invalidated"
        );
    }
}
