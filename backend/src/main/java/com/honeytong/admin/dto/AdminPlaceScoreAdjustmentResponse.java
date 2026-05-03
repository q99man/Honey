package com.honeytong.admin.dto;

import java.math.BigDecimal;

public record AdminPlaceScoreAdjustmentResponse(
        Long placeId,
        String name,
        BigDecimal scoreTotal,
        BigDecimal manualAdjustmentScore
) {
}
