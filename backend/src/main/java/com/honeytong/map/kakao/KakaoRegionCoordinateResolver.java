package com.honeytong.map.kakao;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.service.RegionCoordinateResolver;
import com.honeytong.region.service.ResolvedRegion;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.maps", name = "provider", havingValue = "kakao", matchIfMissing = true)
public class KakaoRegionCoordinateResolver implements RegionCoordinateResolver {

    private final KakaoLocalClient kakaoLocalClient;
    private final RegionDongRepository regionDongRepository;

    public KakaoRegionCoordinateResolver(
            KakaoLocalClient kakaoLocalClient,
            RegionDongRepository regionDongRepository
    ) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.regionDongRepository = regionDongRepository;
    }

    @Override
    public ResolvedRegion resolve(BigDecimal latitude, BigDecimal longitude) {
        KakaoRegionDocument document = kakaoLocalClient.findRegionCodes(latitude, longitude).stream()
                .filter(KakaoRegionDocument::isAdministrativeRegion)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "좌표에 해당하는 행정동을 찾을 수 없습니다."));

        RegionDong dong = regionDongRepository.findByCode(document.code())
                .or(() -> regionDongRepository.findFirstByCityNameKoAndDistrictNameKoAndNameKo(
                        document.region1DepthName(),
                        document.region2DepthName(),
                        document.region3DepthName()
                ))
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "카카오 행정동 코드와 매칭되는 지역 데이터가 없습니다."
                ));

        return new ResolvedRegion(dong);
    }
}
