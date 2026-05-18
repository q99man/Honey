package com.honeytong.admin.dto;

import com.honeytong.user.entity.TrustGrade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdminUserTrustAdjustRequest(
        @NotNull
        @PositiveOrZero
        Integer trustScore,

        @NotNull
        TrustGrade trustGrade,

        String memo
) {
}
