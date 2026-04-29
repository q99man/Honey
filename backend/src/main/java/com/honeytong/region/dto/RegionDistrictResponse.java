package com.honeytong.region.dto;

public record RegionDistrictResponse(
        Long id,
        Long cityId,
        String nameKo,
        String nameEn,
        String nameJa,
        String code
) {
}
