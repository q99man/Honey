package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    private static final long SEASON_ID = 1L;
    private static final long DONG_ID = 30L;
    private static final long PLACE_ID = 100L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private PlaceSeasonScoreRepository placeSeasonScoreRepository;

    @Mock
    private RegionCityRepository regionCityRepository;

    @Mock
    private RegionDistrictRepository regionDistrictRepository;

    @Mock
    private RegionDongRepository regionDongRepository;

    private RankingService rankingService;
    private Season season;
    private RegionCity city;
    private RegionDistrict district;
    private RegionDong dong;
    private Place place;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(
                seasonRepository,
                placeSeasonScoreRepository,
                regionCityRepository,
                regionDistrictRepository,
                regionDongRepository
        );

        season = new Season(
                "2026-04",
                "2026년 4월",
                SeasonType.MONTHLY,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                SeasonStatus.ACTIVE
        );
        ReflectionTestUtils.setField(season, "id", SEASON_ID);

        city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", DONG_ID);

        User user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 5L);
        place = new Place(
                user,
                dong,
                "서교 순대국",
                "KOREAN",
                "서울 마포구 양화로 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "순대국",
                "동네에서 다시 찾고 싶은 국밥집",
                "맑은 국물과 빠른 회전이 좋습니다.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
    }

    @Test
    void getPlaceRankings_readsAggregatedSeasonScores() {
        PlaceSeasonScore score = new PlaceSeasonScore(
                season,
                place,
                RankingRegionType.DONG,
                DONG_ID,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(35),
                1,
                2
        );
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));
        when(regionDongRepository.findById(DONG_ID)).thenReturn(Optional.of(dong));
        when(placeSeasonScoreRepository.findTop50BySeasonIdAndRegionTypeAndRegionRefIdOrderByRankNoAscTotalScoreDesc(
                SEASON_ID,
                RankingRegionType.DONG,
                DONG_ID
        )).thenReturn(List.of(score));

        var response = rankingService.getPlaceRankings("dong", DONG_ID, null);

        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.regionType()).isEqualTo("dong");
        assertThat(response.regionName()).isEqualTo("서교동");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().placeId()).isEqualTo(PLACE_ID);
        assertThat(response.items().getFirst().rank()).isEqualTo(1);
        assertThat(response.items().getFirst().starLevel()).isEqualTo(2);
        assertThat(response.items().getFirst().totalScore()).isEqualByComparingTo("35");
        assertThat(response.items().getFirst().audienceTags()).isEmpty();
    }

    @Test
    void getPlaceRankings_usesRequestedSeasonCodeWhenProvided() {
        when(seasonRepository.findBySeasonCode("2026-04")).thenReturn(Optional.of(season));
        when(regionDongRepository.findById(DONG_ID)).thenReturn(Optional.of(dong));
        when(placeSeasonScoreRepository.findTop50BySeasonIdAndRegionTypeAndRegionRefIdOrderByRankNoAscTotalScoreDesc(
                SEASON_ID,
                RankingRegionType.DONG,
                DONG_ID
        )).thenReturn(List.of());

        var response = rankingService.getPlaceRankings("DONG", DONG_ID, "2026-04");

        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getCurrentSeason_returnsActiveSeason() {
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));

        var response = rankingService.getCurrentSeason();

        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.seasonType()).isEqualTo("MONTHLY");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getPlaceRankings_rejectsInvalidRegionType() {
        assertThatThrownBy(() -> rankingService.getPlaceRankings("town", DONG_ID, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void getCurrentSeason_rejectsWhenActiveSeasonDoesNotExist() {
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rankingService.getCurrentSeason())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
