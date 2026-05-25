package com.honeytong.place.controller;

import com.honeytong.auth.security.RequireNoActiveSanction;
import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.common.api.ApiResponse;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.place.dto.PlaceCreateResponse;
import com.honeytong.place.dto.PlaceDeleteResponse;
import com.honeytong.place.dto.PlaceDetailResponse;
import com.honeytong.place.dto.PlaceListItemResponse;
import com.honeytong.place.dto.PlaceRegistrationPolicyResponse;
import com.honeytong.place.dto.PlaceUpdateRequest;
import com.honeytong.place.dto.PlaceUpdateResponse;
import com.honeytong.place.service.PlaceService;
import com.honeytong.ranking.dto.PlaceRankingHistoryResponse;
import com.honeytong.ranking.service.PlaceRankingHistoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final PlaceRankingHistoryService placeRankingHistoryService;

    public PlaceController(PlaceService placeService, PlaceRankingHistoryService placeRankingHistoryService) {
        this.placeService = placeService;
        this.placeRankingHistoryService = placeRankingHistoryService;
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @PostMapping
    public ApiResponse<PlaceCreateResponse> createPlace(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlaceCreateRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        return ApiResponse.success(placeService.createPlace(userId, request, clientIp), "맛집이 등록되었습니다.");
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
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
            @RequestParam(defaultValue = "1000") double radius
    ) {
        return ApiResponse.success(placeService.getNearbyPlaces(lat, lng, (int) Math.round(radius)), "OK");
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

    @GetMapping("/{placeId}/ranking-history")
    public ApiResponse<PlaceRankingHistoryResponse> getPlaceRankingHistory(@PathVariable Long placeId) {
        return ApiResponse.success(placeRankingHistoryService.getPlaceRankingHistory(placeId), "OK");
    }

    @PatchMapping("/{placeId}")
    public ApiResponse<PlaceUpdateResponse> updatePlace(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId,
            @Valid @RequestBody PlaceUpdateRequest request
    ) {
        return ApiResponse.success(placeService.updatePlace(userId, placeId, request), "Place updated");
    }

    @DeleteMapping("/{placeId}")
    public ApiResponse<PlaceDeleteResponse> deletePlace(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(placeService.deletePlace(userId, placeId), "Place deleted");
    }
}
