package com.honeytong.recommendation.dto;

import java.time.LocalDateTime;

public record MyRecommendationResponse(
        Long recommendationId,
        Long placeId,
        String placeName,
        LocalDateTime recommendedAt
) {
}
