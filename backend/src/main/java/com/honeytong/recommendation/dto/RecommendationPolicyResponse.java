package com.honeytong.recommendation.dto;

public record RecommendationPolicyResponse(
        boolean canRecommend,
        String reason,
        int dailyRemainingCount
) {
}
