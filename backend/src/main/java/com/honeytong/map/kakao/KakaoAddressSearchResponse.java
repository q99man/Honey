package com.honeytong.map.kakao;

import java.util.List;

public record KakaoAddressSearchResponse(
        List<KakaoAddressDocument> documents
) {
}
