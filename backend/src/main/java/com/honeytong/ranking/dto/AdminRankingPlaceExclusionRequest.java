package com.honeytong.ranking.dto;

import jakarta.validation.constraints.NotNull;

public record AdminRankingPlaceExclusionRequest(
        @NotNull
        Boolean excluded,

        String memo
) {
}
