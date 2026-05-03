package com.honeytong.place.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceUpdateResponse(
        Long placeId,
        String name,
        String categoryCode,
        Long cityId,
        Long districtId,
        Long dongId,
        String addressRoad,
        String addressJibun,
        BigDecimal latitude,
        BigDecimal longitude,
        String priceRangeCode,
        String recommendedMenu,
        String shortRecommendation,
        String featureText,
        boolean franchise,
        List<String> imageUrls
) {
}
