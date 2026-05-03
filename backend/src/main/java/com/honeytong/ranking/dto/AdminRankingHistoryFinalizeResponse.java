package com.honeytong.ranking.dto;

public record AdminRankingHistoryFinalizeResponse(
        Long seasonId,
        String seasonCode,
        int historyCount
) {
}
