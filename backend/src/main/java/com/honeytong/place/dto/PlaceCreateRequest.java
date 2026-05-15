package com.honeytong.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PlaceCreateRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(max = 50)
        String categoryCode,

        @NotNull
        Long dongId,

        @Size(max = 255)
        String addressRoad,

        @Size(max = 255)
        String addressJibun,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude,

        @Size(max = 30)
        String priceRangeCode,

        String recommendedMenu,

        @NotBlank
        String shortRecommendation,

        String featureText,

        boolean franchise,

        List<@Size(max = 255) String> imageUrls
) {
}
