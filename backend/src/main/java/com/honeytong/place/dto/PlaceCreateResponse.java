package com.honeytong.place.dto;

import com.honeytong.place.entity.PlaceApprovalStatus;

public record PlaceCreateResponse(
        Long placeId,
        PlaceApprovalStatus approvalStatus
) {
}
