package com.honeytong.ranking.dto;

import java.time.LocalDateTime;

public record CurrentSeasonResponse(
        String seasonCode,
        String seasonName,
        String seasonType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status
) {
}
