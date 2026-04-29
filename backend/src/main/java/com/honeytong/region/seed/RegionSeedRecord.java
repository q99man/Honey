package com.honeytong.region.seed;

public record RegionSeedRecord(
        String cityCode,
        String cityNameKo,
        String districtCode,
        String districtNameKo,
        String dongCode,
        String dongNameKo
) {

    public RegionSeedRecord {
        cityCode = require(cityCode, "city_code");
        cityNameKo = require(cityNameKo, "city_name_ko");
        districtCode = defaultIfBlank(districtCode, cityCode);
        districtNameKo = defaultIfBlank(districtNameKo, cityNameKo);
        dongCode = require(dongCode, "dong_code");
        dongNameKo = require(dongNameKo, "dong_name_ko");
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue.trim();
        }
        return value.trim();
    }
}
