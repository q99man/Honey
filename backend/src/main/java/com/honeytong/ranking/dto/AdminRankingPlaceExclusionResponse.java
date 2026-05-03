package com.honeytong.ranking.dto;

public record AdminRankingPlaceExclusionResponse(
        Long placeId,
        String name,
        boolean rankingExcluded,
        int starLevel
) {
}
