package com.honeytong.ranking.dto;

import java.util.List;

public record PlaceRankingResponse(
        String seasonCode,
        String regionType,
        String regionName,
        List<PlaceRankingItemResponse> items
) {
}
