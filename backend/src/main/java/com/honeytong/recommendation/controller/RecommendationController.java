package com.honeytong.recommendation.controller;

import com.honeytong.auth.security.RequireNoActiveSanction;
import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.common.api.ApiResponse;
import com.honeytong.recommendation.dto.MyRecommendationResponse;
import com.honeytong.recommendation.dto.RecommendationPolicyResponse;
import com.honeytong.recommendation.dto.RecommendationResponse;
import com.honeytong.recommendation.service.RecommendationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @PostMapping("/places/{placeId}/recommend")
    public ApiResponse<RecommendationResponse> recommend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        return ApiResponse.success(
                recommendationService.recommend(userId, placeId, clientIp),
                "Recommendation completed"
        );
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @DeleteMapping("/places/{placeId}/recommend")
    public ApiResponse<RecommendationResponse> cancelRecommendation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(
                recommendationService.cancelRecommendation(userId, placeId),
                "Recommendation canceled"
        );
    }

    @GetMapping("/places/{placeId}/recommend-policy")
    public ApiResponse<RecommendationPolicyResponse> getRecommendationPolicy(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(
                recommendationService.getRecommendationPolicy(userId, placeId),
                "OK"
        );
    }

    @GetMapping("/users/me/recommendations")
    public ApiResponse<List<MyRecommendationResponse>> getMyRecommendations(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(recommendationService.getMyRecommendations(userId), "OK");
    }
}
