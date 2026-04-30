package com.honeytong.recommendation.dto;

import java.math.BigDecimal;

public record RecommendationResponse(
        boolean recommended,
        int recommendCount,
        BigDecimal myWeight
) {
}
