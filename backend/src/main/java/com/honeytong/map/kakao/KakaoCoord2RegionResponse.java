package com.honeytong.map.kakao;

import java.util.List;

public record KakaoCoord2RegionResponse(
        List<KakaoRegionDocument> documents
) {
}
