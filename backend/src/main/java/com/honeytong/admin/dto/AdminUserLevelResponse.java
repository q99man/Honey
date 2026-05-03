package com.honeytong.admin.dto;

import java.math.BigDecimal;

public record AdminUserLevelResponse(
        int level,
        int exp,
        int totalExp,
        String title,
        String avatarStage,
        BigDecimal rankScore
) {
}
