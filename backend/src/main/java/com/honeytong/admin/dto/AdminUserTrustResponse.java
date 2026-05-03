package com.honeytong.admin.dto;

import com.honeytong.user.entity.TrustGrade;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserTrustResponse(
        int trustScore,
        TrustGrade trustGrade,
        BigDecimal recommendWeight,
        int sanctionCount,
        int reportReceivedCount,
        int reportConfirmedCount,
        int abnormalActivityScore,
        boolean phoneVerified,
        boolean regionVerified,
        LocalDateTime lastEvaluatedAt
) {
}
