package com.honeytong.region.dto;

public record RegionCityResponse(
        Long id,
        String nameKo,
        String nameEn,
        String nameJa,
        String code
) {
}
