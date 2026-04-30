package com.honeytong.visit.dto;

import java.time.LocalDateTime;

public record PlaceVisitSummaryResponse(
        Long placeId,
        int visitCount,
        LocalDateTime lastVisitedAt
) {
}
