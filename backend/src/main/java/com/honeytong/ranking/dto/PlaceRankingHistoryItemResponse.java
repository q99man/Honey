package com.honeytong.ranking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceRankingHistoryItemResponse(
        Long seasonId,
        String seasonCode,
        String seasonName,
        String seasonType,
        LocalDateTime seasonStartAt,
        LocalDateTime seasonEndAt,
        String regionType,
        Long regionId,
        int rank,
        int starLevel,
        BigDecimal totalScore,
        LocalDateTime finalizedAt
) {
}
