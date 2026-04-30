package com.honeytong.policy.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.policy.dto.AdminPolicyResponse;
import com.honeytong.policy.dto.AdminPolicyUpdateRequest;
import com.honeytong.policy.dto.AdminRegionPolicyRequest;
import com.honeytong.policy.dto.AdminRegionPolicyResponse;
import com.honeytong.policy.service.AdminPolicyService;
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
@RequestMapping("/api/admin/policies")
public class AdminPolicyController {

    private final AdminPolicyService adminPolicyService;

    public AdminPolicyController(AdminPolicyService adminPolicyService) {
        this.adminPolicyService = adminPolicyService;
    }

    @GetMapping
    public ApiResponse<List<AdminPolicyResponse>> getPolicies(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminPolicyService.getPolicies(adminUserId), "OK");
    }

    @PatchMapping("/{policyKey}")
    public ApiResponse<AdminPolicyResponse> updatePolicy(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable String policyKey,
            @Valid @RequestBody AdminPolicyUpdateRequest request
    ) {
        return ApiResponse.success(adminPolicyService.updatePolicy(adminUserId, policyKey, request), "Policy updated");
    }

    @GetMapping("/region")
    public ApiResponse<AdminRegionPolicyResponse> getRegionPolicy(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminPolicyService.getRegionPolicy(adminUserId), "OK");
    }

    @PatchMapping("/region")
    public ApiResponse<AdminRegionPolicyResponse> updateRegionPolicy(
            @AuthenticationPrincipal Long adminUserId,
            @Valid @RequestBody AdminRegionPolicyRequest request
    ) {
        return ApiResponse.success(
                adminPolicyService.updateRegionPolicy(adminUserId, request),
                "Region policy updated"
        );
    }
}
