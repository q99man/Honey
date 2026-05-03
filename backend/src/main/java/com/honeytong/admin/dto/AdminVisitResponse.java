package com.honeytong.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminVisitResponse(
        Long visitId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        String categoryCode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distanceMeter,
        String imageUrl,
        boolean valid,
        String validReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
