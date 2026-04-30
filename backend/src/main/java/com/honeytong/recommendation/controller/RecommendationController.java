package com.honeytong.recommendation.controller;

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
    @PostMapping("/places/{placeId}/recommend")
    public ApiResponse<RecommendationResponse> recommend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(
                recommendationService.recommend(userId, placeId),
                "Recommendation completed"
        );
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
