package com.honeytong.region.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.region.dto.MyRegionResponse;
import com.honeytong.region.dto.RegionChangePolicyResponse;
import com.honeytong.region.dto.RegionChangeRequest;
import com.honeytong.region.dto.RegionCityResponse;
import com.honeytong.region.dto.RegionDistrictResponse;
import com.honeytong.region.dto.RegionDongResponse;
import com.honeytong.region.dto.RegionVerificationResponse;
import com.honeytong.region.dto.RegionVerifyRequest;
import com.honeytong.region.service.RegionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping("/cities")
    public ApiResponse<List<RegionCityResponse>> getCities() {
        return ApiResponse.success(regionService.getCities(), "OK");
    }

    @GetMapping("/cities/{cityId}/districts")
    public ApiResponse<List<RegionDistrictResponse>> getDistricts(@PathVariable Long cityId) {
        return ApiResponse.success(regionService.getDistricts(cityId), "OK");
    }

    @GetMapping("/districts/{districtId}/dongs")
    public ApiResponse<List<RegionDongResponse>> getDongs(@PathVariable Long districtId) {
        return ApiResponse.success(regionService.getDongs(districtId), "OK");
    }

    @PostMapping("/verify")
    public ApiResponse<RegionVerificationResponse> verifyRegion(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegionVerifyRequest request
    ) {
        return ApiResponse.success(regionService.verifyRegion(userId, request), "Region verified");
    }

    @GetMapping("/me")
    public ApiResponse<MyRegionResponse> getMyRegion(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(regionService.getMyRegion(userId), "OK");
    }

    @PatchMapping("/me")
    public ApiResponse<MyRegionResponse> changeMyRegion(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegionChangeRequest request
    ) {
        return ApiResponse.success(regionService.changeMyRegion(userId, request), "Region changed");
    }

    @GetMapping("/me/change-policy")
    public ApiResponse<RegionChangePolicyResponse> getRegionChangePolicy(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(regionService.getRegionChangePolicy(userId), "OK");
    }
}
