package com.honeytong.map.kakao;

import java.math.BigDecimal;
import java.util.List;

public interface KakaoLocalClient {

    List<KakaoRegionDocument> findRegionCodes(BigDecimal latitude, BigDecimal longitude);
}
