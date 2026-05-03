package com.honeytong.admin.dto;

import com.honeytong.recommendation.entity.RecommendationStatus;

public record AdminRecommendationInvalidationResponse(
        Long recommendationId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        RecommendationStatus status,
        int recommendCount
) {
}
