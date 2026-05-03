package com.honeytong.admin.dto;

import com.honeytong.user.entity.TrustGrade;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserTrustAdjustResponse(
        Long userId,
        String nickname,
        int trustScore,
        TrustGrade trustGrade,
        BigDecimal recommendWeight,
        LocalDateTime lastEvaluatedAt
) {
}
