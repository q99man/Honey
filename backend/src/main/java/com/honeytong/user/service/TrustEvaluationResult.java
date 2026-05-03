package com.honeytong.user.service;

import com.honeytong.user.entity.TrustGrade;
import java.math.BigDecimal;

public record TrustEvaluationResult(
        TrustGrade trustGrade,
        BigDecimal recommendWeight
) {
}
