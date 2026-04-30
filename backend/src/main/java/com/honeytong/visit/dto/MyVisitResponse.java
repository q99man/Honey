package com.honeytong.visit.dto;

import java.time.LocalDateTime;

public record MyVisitResponse(
        Long visitId,
        Long placeId,
        String placeName,
        String categoryCode,
        String address,
        Integer distanceMeter,
        String imageUrl,
        LocalDateTime visitedAt
) {
}
