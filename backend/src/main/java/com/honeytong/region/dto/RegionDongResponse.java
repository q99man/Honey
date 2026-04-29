package com.honeytong.region.dto;

public record RegionDongResponse(
        Long id,
        Long cityId,
        Long districtId,
        String nameKo,
        String nameEn,
        String nameJa,
        String code
) {
}
