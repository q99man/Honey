package com.honeytong.map.kakao;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.map.config.MapProperties;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.maps", name = "provider", havingValue = "kakao", matchIfMissing = true)
public class KakaoLocalRestClient implements KakaoLocalClient {

    private final MapProperties mapProperties;

    public KakaoLocalRestClient(MapProperties mapProperties) {
        this.mapProperties = mapProperties;
    }

    @Override
    public List<KakaoRegionDocument> findRegionCodes(BigDecimal latitude, BigDecimal longitude) {
        validateConfiguration();

        try {
            KakaoCoord2RegionResponse response = restClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("input_coord", "WGS84")
                            .queryParam("output_coord", "WGS84")
                            .build()
                    )
                    .retrieve()
                    .body(KakaoCoord2RegionResponse.class);

            if (response == null || response.documents() == null) {
                return List.of();
            }
            return response.documents();
        } catch (RestClientException ex) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "카카오 지역 코드 조회에 실패했습니다.");
        }
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(mapProperties.kakao().localBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + mapProperties.kakao().restApiKey())
                .build();
    }

    private void validateConfiguration() {
        if (mapProperties.kakao() == null || !StringUtils.hasText(mapProperties.kakao().restApiKey())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "카카오 REST API 키가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(mapProperties.kakao().localBaseUrl())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "카카오 Local API URL이 설정되지 않았습니다.");
        }
    }
}
