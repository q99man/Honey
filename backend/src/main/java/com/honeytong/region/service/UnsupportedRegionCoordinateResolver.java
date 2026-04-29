package com.honeytong.region.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(RegionCoordinateResolver.class)
public class UnsupportedRegionCoordinateResolver implements RegionCoordinateResolver {

    @Override
    public ResolvedRegion resolve(BigDecimal latitude, BigDecimal longitude) {
        throw new ApiException(
                ErrorCode.INVALID_REQUEST,
                "GPS 기반 행정동 매핑 데이터가 아직 설정되지 않았습니다."
        );
    }
}
