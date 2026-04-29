package com.honeytong.user.dto;

public record UserGrowthResponse(
        int level,
        int exp,
        String title,
        String avatarStage
) {
}
