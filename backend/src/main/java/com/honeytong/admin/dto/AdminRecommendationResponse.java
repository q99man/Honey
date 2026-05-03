package com.honeytong.admin.dto;

import com.honeytong.recommendation.entity.RecommendationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRecommendationResponse(
        Long recommendationId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        String categoryCode,
        RecommendationStatus status,
        BigDecimal recommendWeight,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
