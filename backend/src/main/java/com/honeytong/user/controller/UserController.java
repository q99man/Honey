package com.honeytong.user.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.user.dto.UserActivitySummaryResponse;
import com.honeytong.user.dto.UserGrowthResponse;
import com.honeytong.user.dto.UserProfileResponse;
import com.honeytong.user.dto.UserProfileUpdateRequest;
import com.honeytong.user.dto.UserStatusResponse;
import com.honeytong.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId), "OK");
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateMyProfile(userId, request), "Profile updated");
    }

    @GetMapping("/me/status")
    public ApiResponse<UserStatusResponse> getMyStatus(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyStatus(userId), "OK");
    }

    @GetMapping("/me/growth")
    public ApiResponse<UserGrowthResponse> getMyGrowth(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyGrowth(userId), "OK");
    }

    @GetMapping("/me/activity-summary")
    public ApiResponse<UserActivitySummaryResponse> getMyActivitySummary(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyActivitySummary(userId), "OK");
    }
}
