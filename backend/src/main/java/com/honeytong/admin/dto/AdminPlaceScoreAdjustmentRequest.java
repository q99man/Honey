package com.honeytong.admin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminPlaceScoreAdjustmentRequest(
        @NotNull
        @Digits(integer = 10, fraction = 2)
        BigDecimal scoreDelta,

        String memo
) {
}
