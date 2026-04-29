package com.honeytong.region.dto;

import jakarta.validation.constraints.NotNull;

public record RegionChangeRequest(
        @NotNull(message = "동 ID는 필수입니다.")
        Long dongId
) {
}
