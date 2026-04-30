package com.honeytong.ranking.dto;

import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import java.time.LocalDateTime;

public record AdminSeasonUpdateRequest(
        String seasonName,
        SeasonType seasonType,
        LocalDateTime startAt,
        LocalDateTime endAt,
        SeasonStatus status,
        String memo
) {
}
