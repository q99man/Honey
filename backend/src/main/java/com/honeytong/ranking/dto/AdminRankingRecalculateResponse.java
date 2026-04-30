package com.honeytong.ranking.dto;

public record AdminRankingRecalculateResponse(
        String seasonCode,
        int placeCount,
        int scoreCount
) {
}
