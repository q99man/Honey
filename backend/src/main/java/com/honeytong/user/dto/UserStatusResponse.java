package com.honeytong.user.dto;

import java.math.BigDecimal;

public record UserStatusResponse(
        int level,
        int exp,
        int nextLevelExp,
        String trustGrade,
        BigDecimal recommendWeight,
        boolean phoneVerified,
        boolean regionVerified
) {
}
