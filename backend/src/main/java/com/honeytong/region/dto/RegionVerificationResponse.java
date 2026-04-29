package com.honeytong.region.dto;

public record RegionVerificationResponse(
        Long cityId,
        Long districtId,
        Long dongId,
        String cityName,
        String districtName,
        String dongName,
        boolean verified
) {
}
