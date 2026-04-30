package com.honeytong.ranking.dto;

import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AdminSeasonCreateRequest(
        @NotBlank String seasonCode,
        @NotBlank String seasonName,
        @NotNull SeasonType seasonType,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull SeasonStatus status,
        String memo
) {
}
