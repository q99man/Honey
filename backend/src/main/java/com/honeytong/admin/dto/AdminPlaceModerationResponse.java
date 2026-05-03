package com.honeytong.admin.dto;

import com.honeytong.place.entity.FranchiseReviewStatus;
import com.honeytong.place.entity.PlaceApprovalStatus;
import com.honeytong.place.entity.PlaceExposureStatus;

public record AdminPlaceModerationResponse(
        Long placeId,
        String name,
        PlaceApprovalStatus approvalStatus,
        PlaceExposureStatus exposureStatus,
        FranchiseReviewStatus franchiseReviewStatus
) {
}
