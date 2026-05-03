package com.honeytong.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PlaceUpdateRequest(
        @Size(max = 120)
        String name,

        @Size(max = 50)
        String categoryCode,

        Long dongId,

        @Size(max = 255)
        String addressRoad,

        @Size(max = 255)
        String addressJibun,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude,

        @Size(max = 30)
        String priceRangeCode,

        @Size(max = 255)
        String recommendedMenu,

        @Size(max = 255)
        String shortRecommendation,

        @Size(max = 500)
        String featureText,

        Boolean franchise,

        List<@Size(max = 255) String> imageUrls
) {
}
