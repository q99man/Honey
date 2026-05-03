package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminPlaceApprovalStatusRequest;
import com.honeytong.admin.dto.AdminPlaceDetailResponse;
import com.honeytong.admin.dto.AdminPlaceExposureStatusRequest;
import com.honeytong.admin.dto.AdminPlaceFranchiseStatusRequest;
import com.honeytong.admin.dto.AdminPlaceListItemResponse;
import com.honeytong.admin.dto.AdminPlaceModerationResponse;
import com.honeytong.admin.dto.AdminPlaceScoreAdjustmentRequest;
import com.honeytong.admin.dto.AdminPlaceScoreAdjustmentResponse;
import com.honeytong.admin.service.AdminPlaceService;
import com.honeytong.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/places")
public class AdminPlaceController {

    private final AdminPlaceService adminPlaceService;

    public AdminPlaceController(AdminPlaceService adminPlaceService) {
        this.adminPlaceService = adminPlaceService;
    }

    @GetMapping
    public ApiResponse<List<AdminPlaceListItemResponse>> getPlaces(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminPlaceService.getPlaces(adminUserId), "OK");
    }

    @GetMapping("/{placeId}")
    public ApiResponse<AdminPlaceDetailResponse> getPlace(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(adminPlaceService.getPlace(adminUserId, placeId), "OK");
    }

    @PatchMapping("/{placeId}/exposure")
    public ApiResponse<AdminPlaceModerationResponse> changeExposureStatus(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId,
            @Valid @RequestBody AdminPlaceExposureStatusRequest request
    ) {
        return ApiResponse.success(
                adminPlaceService.changeExposureStatus(adminUserId, placeId, request),
                "Place exposure status changed"
        );
    }

    @PatchMapping("/{placeId}/approval")
    public ApiResponse<AdminPlaceModerationResponse> changeApprovalStatus(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId,
            @Valid @RequestBody AdminPlaceApprovalStatusRequest request
    ) {
        return ApiResponse.success(
                adminPlaceService.changeApprovalStatus(adminUserId, placeId, request),
                "Place approval status changed"
        );
    }

    @PatchMapping("/{placeId}/franchise-status")
    public ApiResponse<AdminPlaceModerationResponse> changeFranchiseStatus(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId,
            @Valid @RequestBody AdminPlaceFranchiseStatusRequest request
    ) {
        return ApiResponse.success(
                adminPlaceService.changeFranchiseStatus(adminUserId, placeId, request),
                "Place franchise review status changed"
        );
    }

    @PatchMapping("/{placeId}/score-adjustment")
    public ApiResponse<AdminPlaceScoreAdjustmentResponse> adjustScore(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId,
            @Valid @RequestBody AdminPlaceScoreAdjustmentRequest request
    ) {
        return ApiResponse.success(
                adminPlaceService.adjustScore(adminUserId, placeId, request),
                "Place score adjusted"
        );
    }
}
