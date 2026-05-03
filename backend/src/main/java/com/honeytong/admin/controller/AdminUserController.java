package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminUserDetailResponse;
import com.honeytong.admin.dto.AdminUserListItemResponse;
import com.honeytong.admin.dto.AdminUserRecommendWeightRequest;
import com.honeytong.admin.dto.AdminUserSanctionRequest;
import com.honeytong.admin.dto.AdminUserSanctionResponse;
import com.honeytong.admin.dto.AdminUserTrustAdjustRequest;
import com.honeytong.admin.dto.AdminUserTrustAdjustResponse;
import com.honeytong.admin.service.AdminUserService;
import com.honeytong.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserListItemResponse>> getUsers(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminUserService.getUsers(adminUserId), "OK");
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> getUser(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long userId
    ) {
        return ApiResponse.success(adminUserService.getUser(adminUserId, userId), "OK");
    }

    @PostMapping("/{userId}/sanctions")
    public ApiResponse<AdminUserSanctionResponse> createSanction(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserSanctionRequest request
    ) {
        return ApiResponse.success(
                adminUserService.createSanction(adminUserId, userId, request),
                "User sanction created"
        );
    }

    @PatchMapping("/{userId}/trust")
    public ApiResponse<AdminUserTrustAdjustResponse> adjustTrust(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserTrustAdjustRequest request
    ) {
        return ApiResponse.success(
                adminUserService.adjustTrust(adminUserId, userId, request),
                "User trust adjusted"
        );
    }

    @PatchMapping("/{userId}/recommend-weight")
    public ApiResponse<AdminUserTrustAdjustResponse> adjustRecommendWeight(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRecommendWeightRequest request
    ) {
        return ApiResponse.success(
                adminUserService.adjustRecommendWeight(adminUserId, userId, request),
                "User recommendation weight adjusted"
        );
    }
}
