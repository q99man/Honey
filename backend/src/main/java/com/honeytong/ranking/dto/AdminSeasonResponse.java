package com.honeytong.ranking.dto;

import com.honeytong.ranking.entity.Season;
import java.time.LocalDateTime;

public record AdminSeasonResponse(
        Long id,
        String seasonCode,
        String seasonName,
        String seasonType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status
) {

    public static AdminSeasonResponse from(Season season) {
        return new AdminSeasonResponse(
                season.getId(),
                season.getSeasonCode(),
                season.getSeasonName(),
                season.getSeasonType().name(),
                season.getStartAt(),
                season.getEndAt(),
                season.getStatus().name()
        );
    }
}
