package com.honeytong.admin.dto;

import com.honeytong.place.entity.PlaceApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record AdminPlaceApprovalStatusRequest(
        @NotNull PlaceApprovalStatus approvalStatus,
        String memo
) {
}
