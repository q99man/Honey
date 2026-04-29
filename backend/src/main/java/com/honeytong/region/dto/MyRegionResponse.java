package com.honeytong.region.dto;

public record MyRegionResponse(
        Long cityId,
        Long districtId,
        Long dongId,
        String cityName,
        String districtName,
        String dongName,
        boolean verified
) {
}
