package com.honeytong.place.dto;

import com.honeytong.place.entity.FranchiseReviewStatus;
import com.honeytong.place.entity.PlaceApprovalStatus;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailResponse(
        Long id,
        String name,
        String categoryCode,
        Long cityId,
        Long districtId,
        Long dongId,
        String cityName,
        String districtName,
        String dongName,
        String addressRoad,
        String addressJibun,
        BigDecimal latitude,
        BigDecimal longitude,
        String priceRangeCode,
        String recommendedMenu,
        String shortRecommendation,
        String featureText,
        boolean franchise,
        FranchiseReviewStatus franchiseReviewStatus,
        PlaceApprovalStatus approvalStatus,
        PlaceExposureStatus exposureStatus,
        int starLevel,
        String flowerGrade,
        int recommendCount,
        int visitCount,
        int commentCount,
        List<String> imageUrls,
        List<String> audienceTags,
        List<String> aiTags
) {
}
