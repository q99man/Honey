package com.honeytong.analytics.dto;

import java.util.List;

public record RegionalTrendsResponse(
        List<TrendingPlace> trendingPlaces,
        List<RisingCategory> risingCategories
) {
    public record TrendingPlace(
            Long placeId,
            String placeName,
            long activityCount
    ) {}

    public record RisingCategory(
            String categoryCode,
            long activityCount
    ) {}
}
