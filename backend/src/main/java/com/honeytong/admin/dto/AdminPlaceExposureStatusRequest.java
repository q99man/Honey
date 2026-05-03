package com.honeytong.admin.dto;

import com.honeytong.place.entity.PlaceExposureStatus;
import jakarta.validation.constraints.NotNull;

public record AdminPlaceExposureStatusRequest(
        @NotNull PlaceExposureStatus exposureStatus,
        String memo
) {
}
