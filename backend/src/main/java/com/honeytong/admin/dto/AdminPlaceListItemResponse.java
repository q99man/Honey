package com.honeytong.admin.dto;

import com.honeytong.place.entity.FranchiseReviewStatus;
import com.honeytong.place.entity.PlaceApprovalStatus;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.time.LocalDateTime;

public record AdminPlaceListItemResponse(
        Long placeId,
        String name,
        String categoryCode,
        Long createdByUserId,
        String createdByNickname,
        Long cityId,
        Long districtId,
        Long dongId,
        String cityName,
        String districtName,
        String dongName,
        boolean franchise,
        FranchiseReviewStatus franchiseReviewStatus,
        PlaceApprovalStatus approvalStatus,
        PlaceExposureStatus exposureStatus,
        boolean rankingExcluded,
        int starLevel,
        String flowerGrade,
        int recommendCount,
        int visitCount,
        int commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
