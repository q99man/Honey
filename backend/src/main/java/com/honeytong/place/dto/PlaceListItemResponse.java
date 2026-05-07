package com.honeytong.place.dto;

import java.math.BigDecimal;

public record PlaceListItemResponse(
        Long id,
        String name,
        String categoryCode,
        Long cityId,
        Long districtId,
        Long dongId,
        String regionName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String shortRecommendation,
        int starLevel,
        String flowerGrade,
        int recommendCount,
        int visitCount,
        int commentCount,
        Integer distanceMeter,
        String representativeImageUrl
) {
}
