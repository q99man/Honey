package com.honeytong.ranking.dto;

import java.util.List;

public record PlaceRankingHistoryResponse(
        Long placeId,
        String name,
        List<PlaceRankingHistoryItemResponse> items
) {
}
