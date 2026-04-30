package com.honeytong.place.controller;

import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.common.api.ApiResponse;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.place.dto.PlaceCreateResponse;
import com.honeytong.place.dto.PlaceDetailResponse;
import com.honeytong.place.dto.PlaceListItemResponse;
import com.honeytong.place.dto.PlaceRegistrationPolicyResponse;
import com.honeytong.place.service.PlaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @RequirePhoneVerified
    @PostMapping
    public ApiResponse<PlaceCreateResponse> createPlace(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaceCreateRequest request
    ) {
        return ApiResponse.success(placeService.createPlace(userId, request), "Place created");
    }

    @GetMapping
    public ApiResponse<List<PlaceListItemResponse>> getPlaces(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) Long dongId
    ) {
        return ApiResponse.success(placeService.getPlaces(cityId, districtId, dongId), "OK");
    }

    @GetMapping("/nearby")
    public ApiResponse<List<PlaceListItemResponse>> getNearbyPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") int radius
    ) {
        return ApiResponse.success(placeService.getNearbyPlaces(lat, lng, radius), "OK");
    }

    @GetMapping("/search")
    public ApiResponse<List<PlaceListItemResponse>> searchPlaces(@RequestParam String keyword) {
        return ApiResponse.success(placeService.searchPlaces(keyword), "OK");
    }

    @GetMapping("/registration-policy")
    public ApiResponse<PlaceRegistrationPolicyResponse> getRegistrationPolicy(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(placeService.getRegistrationPolicy(userId), "OK");
    }

    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailResponse> getPlace(@PathVariable Long placeId) {
        return ApiResponse.success(placeService.getPlace(placeId), "OK");
    }
}
