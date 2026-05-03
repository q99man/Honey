package com.honeytong.ranking.dto;

import jakarta.validation.constraints.Size;

public record AdminRankingHistoryFinalizeRequest(
        @Size(max = 255)
        String memo
) {
}
