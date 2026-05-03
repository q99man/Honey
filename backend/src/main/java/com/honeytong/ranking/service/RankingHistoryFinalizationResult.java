package com.honeytong.ranking.service;

public record RankingHistoryFinalizationResult(
        Long seasonId,
        String seasonCode,
        int historyCount
) {
}
