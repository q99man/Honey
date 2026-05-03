package com.honeytong.ranking.service;

public record RankingRecalculationResult(
        Long seasonId,
        String seasonCode,
        int placeCount,
        int scoreCount
) {
}
