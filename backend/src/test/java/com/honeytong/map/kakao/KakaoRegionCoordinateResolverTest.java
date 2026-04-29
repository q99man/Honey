package com.honeytong.map.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.repository.RegionDongRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KakaoRegionCoordinateResolverTest {

    @Mock
    private KakaoLocalClient kakaoLocalClient;

    @Mock
    private RegionDongRepository regionDongRepository;

    private KakaoRegionCoordinateResolver resolver;
    private RegionDong dong;

    @BeforeEach
    void setUp() {
        RegionCity city = new RegionCity("경기도", "Gyeonggi-do", null, "41");
        ReflectionTestUtils.setField(city, "id", 1L);
        RegionDistrict district = new RegionDistrict(city, "성남시 분당구", "Bundang-gu", null, "41135");
        ReflectionTestUtils.setField(district, "id", 2L);
        dong = new RegionDong(city, district, "삼평동", "Sampyeong-dong", null, "4113565500");
        ReflectionTestUtils.setField(dong, "id", 3L);

        resolver = new KakaoRegionCoordinateResolver(kakaoLocalClient, regionDongRepository);
    }

    @Test
    void resolve_prefersAdministrativeRegionAndMatchesByCode() {
        BigDecimal latitude = BigDecimal.valueOf(37.4012191);
        BigDecimal longitude = BigDecimal.valueOf(127.1086228);
        KakaoRegionDocument legalRegion = new KakaoRegionDocument(
                "B",
                "경기도 성남시 분당구 삼평동",
                "경기도",
                "성남시 분당구",
                "삼평동",
                "",
                "4113510900"
        );
        KakaoRegionDocument administrativeRegion = new KakaoRegionDocument(
                "H",
                "경기도 성남시 분당구 삼평동",
                "경기도",
                "성남시 분당구",
                "삼평동",
                "",
                "4113565500"
        );

        when(kakaoLocalClient.findRegionCodes(latitude, longitude))
                .thenReturn(List.of(legalRegion, administrativeRegion));
        when(regionDongRepository.findByCode("4113565500")).thenReturn(Optional.of(dong));

        var resolved = resolver.resolve(latitude, longitude);

        assertThat(resolved.dong()).isEqualTo(dong);
    }
}
