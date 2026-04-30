package com.honeytong.place.dto;

public record PlaceListItemResponse(
        Long id,
        String name,
        String categoryCode,
        Long cityId,
        Long districtId,
        Long dongId,
        String regionName,
        String address,
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
