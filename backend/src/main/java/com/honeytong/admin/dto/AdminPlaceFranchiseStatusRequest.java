package com.honeytong.admin.dto;

import com.honeytong.place.entity.FranchiseReviewStatus;
import jakarta.validation.constraints.NotNull;

public record AdminPlaceFranchiseStatusRequest(
        @NotNull FranchiseReviewStatus franchiseReviewStatus,
        String memo
) {
}
