package com.honeytong.admin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AdminUserRecommendWeightRequest(
        @NotNull
        @PositiveOrZero
        @Digits(integer = 2, fraction = 2)
        BigDecimal recommendWeight,

        String memo
) {
}
