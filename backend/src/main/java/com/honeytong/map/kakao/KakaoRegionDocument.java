package com.honeytong.map.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoRegionDocument(
        @JsonProperty("region_type")
        String regionType,

        @JsonProperty("address_name")
        String addressName,

        @JsonProperty("region_1depth_name")
        String region1DepthName,

        @JsonProperty("region_2depth_name")
        String region2DepthName,

        @JsonProperty("region_3depth_name")
        String region3DepthName,

        @JsonProperty("region_4depth_name")
        String region4DepthName,

        String code
) {

    public boolean isAdministrativeRegion() {
        return "H".equals(regionType);
    }
}
