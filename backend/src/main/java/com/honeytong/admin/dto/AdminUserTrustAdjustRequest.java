package com.honeytong.admin.dto;

import com.honeytong.user.entity.TrustGrade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminUserTrustAdjustRequest(
        @NotNull
        @PositiveOrZero
        Integer trustScore,

        @NotNull
        TrustGrade trustGrade,

        @Size(max = 255)
        String memo
) {
}
