package com.honeytong.ranking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRankingPlaceExclusionRequest(
        @NotNull
        Boolean excluded,

        @Size(max = 255)
        String memo
) {
}
