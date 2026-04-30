package com.honeytong.ranking.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceRankingItemResponse(
        int rank,
        Long placeId,
        String name,
        int starLevel,
        BigDecimal totalScore,
        List<String> audienceTags
) {
}
