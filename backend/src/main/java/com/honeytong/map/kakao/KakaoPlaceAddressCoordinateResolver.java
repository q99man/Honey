package com.honeytong.map.kakao;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.service.PlaceAddressCoordinateResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.maps", name = "provider", havingValue = "kakao", matchIfMissing = true)
public class KakaoPlaceAddressCoordinateResolver implements PlaceAddressCoordinateResolver {

    private final KakaoLocalClient kakaoLocalClient;

    public KakaoPlaceAddressCoordinateResolver(KakaoLocalClient kakaoLocalClient) {
        this.kakaoLocalClient = kakaoLocalClient;
    }

    @Override
    public ResolvedPlaceCoordinate resolve(String address) {
        if (!StringUtils.hasText(address)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "주소를 입력해 주세요.");
        }

        KakaoAddressDocument document = kakaoLocalClient.findAddressCoordinates(address.trim()).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "주소에 해당하는 좌표를 찾을 수 없습니다."));

        return new ResolvedPlaceCoordinate(document.y(), document.x());
    }
}
