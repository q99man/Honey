package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.ranking.entity.PlaceRankingHistory;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.PlaceRankingHistoryRepository;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RankingHistoryFinalizationServiceTest {

    private static final long SEASON_ID = 10L;
    private static final long PLACE_ID = 100L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private PlaceSeasonScoreRepository placeSeasonScoreRepository;

    @Mock
    private PlaceRankingHistoryRepository placeRankingHistoryRepository;

    private RankingHistoryFinalizationService rankingHistoryFinalizationService;
    private Season season;
    private Place place;

    @BeforeEach
    void setUp() {
        rankingHistoryFinalizationService = new RankingHistoryFinalizationService(
                seasonRepository,
                placeSeasonScoreRepository,
                placeRankingHistoryRepository
        );

        season = new Season(
                "2026-04",
                "April 2026",
                SeasonType.MONTHLY,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                SeasonStatus.CLOSED
        );
        ReflectionTestUtils.setField(season, "id", SEASON_ID);

        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 20L);
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 30L);
        RegionDong dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 40L);

        User owner = new User("owner", "owner@example.com");
        ReflectionTestUtils.setField(owner, "id", 2L);
        place = new Place(
                owner,
                dong,
                "Honey Soup",
                "KOREAN",
                "Mapo road 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Soup",
                "A reliable local soup place",
                "Rich soup and fast service.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void finalizeSeasonHistory_rewritesHistoryFromSeasonScores() {
        PlaceSeasonScore dongScore = score(RankingRegionType.DONG, 40L, 1, 1, "8.25");
        PlaceSeasonScore districtScore = score(RankingRegionType.DISTRICT, 30L, 1, 2, "8.25");
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(placeSeasonScoreRepository.findAllBySeasonIdOrderByRegionTypeAscRegionRefIdAscRankNoAsc(SEASON_ID))
                .thenReturn(List.of(dongScore, districtScore));

        RankingHistoryFinalizationResult result =
                rankingHistoryFinalizationService.finalizeSeasonHistory(SEASON_ID);

        ArgumentCaptor<Iterable<PlaceRankingHistory>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(placeRankingHistoryRepository).deleteBySeasonId(SEASON_ID);
        verify(placeRankingHistoryRepository).saveAll(captor.capture());
        List<PlaceRankingHistory> savedHistories = new ArrayList<>();
        captor.getValue().forEach(savedHistories::add);

        assertThat(result.seasonId()).isEqualTo(SEASON_ID);
        assertThat(result.seasonCode()).isEqualTo("2026-04");
        assertThat(result.historyCount()).isEqualTo(2);
        assertThat(savedHistories).hasSize(2);
        assertThat(savedHistories)
                .extracting(PlaceRankingHistory::getRegionType)
                .containsExactly(RankingRegionType.DONG, RankingRegionType.DISTRICT);
        assertThat(savedHistories)
                .extracting(PlaceRankingHistory::getPlace)
                .containsOnly(place);
        assertThat(savedHistories)
                .extracting(PlaceRankingHistory::getTotalScore)
                .allSatisfy(totalScore -> assertThat(totalScore).isEqualByComparingTo("8.25"));
    }

    @Test
    void finalizeSeasonHistory_deletesExistingSeasonHistoryBeforeSavingToRemainIdempotent() {
        PlaceSeasonScore cityScore = score(RankingRegionType.CITY, 20L, 1, 3, "9.00");
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(placeSeasonScoreRepository.findAllBySeasonIdOrderByRegionTypeAscRegionRefIdAscRankNoAsc(SEASON_ID))
                .thenReturn(List.of(cityScore));

        rankingHistoryFinalizationService.finalizeSeasonHistory(SEASON_ID);
        rankingHistoryFinalizationService.finalizeSeasonHistory(SEASON_ID);

        verify(placeRankingHistoryRepository, org.mockito.Mockito.times(2)).deleteBySeasonId(SEASON_ID);
        verify(placeRankingHistoryRepository, org.mockito.Mockito.times(2)).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void finalizeSeasonHistory_rejectsEmptyScoreSnapshotBeforeDeletingHistory() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(placeSeasonScoreRepository.findAllBySeasonIdOrderByRegionTypeAscRegionRefIdAscRankNoAsc(SEASON_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> rankingHistoryFinalizationService.finalizeSeasonHistory(SEASON_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(placeRankingHistoryRepository, never()).deleteBySeasonId(SEASON_ID);
        verify(placeRankingHistoryRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private PlaceSeasonScore score(
            RankingRegionType regionType,
            Long regionRefId,
            int rankNo,
            int starLevel,
            String totalScore
    ) {
        return new PlaceSeasonScore(
                season,
                place,
                regionType,
                regionRefId,
                BigDecimal.ONE,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(0.5),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(2.75),
                new BigDecimal(totalScore),
                rankNo,
                starLevel
        );
    }
}
