package com.honeytong.admin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminPlaceScoreAdjustmentRequest(
        @NotNull
        @Digits(integer = 10, fraction = 2)
        BigDecimal scoreDelta,

        @Size(max = 255)
        String memo
) {
}
