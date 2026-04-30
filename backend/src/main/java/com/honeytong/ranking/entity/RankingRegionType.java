package com.honeytong.ranking.entity;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;

public enum RankingRegionType {
    DONG,
    DISTRICT,
    CITY;

    public static RankingRegionType from(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "랭킹 지역 유형을 입력해 주세요.");
        }
        try {
            return RankingRegionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "랭킹 지역 유형은 dong, district, city 중 하나여야 합니다.");
        }
    }

    public String toApiValue() {
        return name().toLowerCase();
    }
}
