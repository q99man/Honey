package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.ranking.cache.RankingCache;
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
class RankingRecalculationServiceTest {

    private static final long SEASON_ID = 10L;
    private static final long PLACE_ID = 100L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private PlaceSeasonScoreRepository placeSeasonScoreRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private RankingCache rankingCache;

    private RankingRecalculationService rankingRecalculationService;
    private Season season;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        rankingRecalculationService = new RankingRecalculationService(
                seasonRepository,
                placeStatsRepository,
                placeSeasonScoreRepository,
                policyService,
                rankingCache
        );

        season = new Season(
                "2026-04",
                "April 2026",
                SeasonType.MONTHLY,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                SeasonStatus.ACTIVE
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

        stats = new PlaceStats(place);
        stats.addRecommendation(BigDecimal.ONE);
        stats.addVisit(BigDecimal.valueOf(2));
        stats.addComment(BigDecimal.valueOf(0.5));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_writesScoresFromAggregatedStatsAndEvictsRankingCache() {
        stats.adjustManualScore(BigDecimal.valueOf(1.25));
        stubActiveSeasonAndWeights();
        when(placeStatsRepository.findAll()).thenReturn(List.of(stats));

        RankingRecalculationResult result = rankingRecalculationService.recalculate(null);

        ArgumentCaptor<Iterable<PlaceSeasonScore>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(placeSeasonScoreRepository).deleteBySeasonId(SEASON_ID);
        verify(placeSeasonScoreRepository).saveAll(captor.capture());
        List<PlaceSeasonScore> savedScores = new ArrayList<>();
        captor.getValue().forEach(savedScores::add);

        assertThat(result.seasonCode()).isEqualTo("2026-04");
        assertThat(result.placeCount()).isEqualTo(1);
        assertThat(result.scoreCount()).isEqualTo(3);
        assertThat(savedScores).hasSize(3);
        assertThat(savedScores)
                .extracting(PlaceSeasonScore::getRegionType)
                .containsExactlyInAnyOrder(RankingRegionType.DONG, RankingRegionType.DISTRICT, RankingRegionType.CITY);
        assertThat(savedScores)
                .extracting(PlaceSeasonScore::getTotalScore)
                .allSatisfy(totalScore -> assertThat(totalScore).isEqualByComparingTo("8.25"));
        assertThat(place.getCurrentStarLevel()).isEqualTo(3);
        verify(rankingCache).evictAllPlaceRankings();
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_skipsRankingExcludedPlaces() {
        place.changeRankingExcluded(true);
        stubActiveSeasonAndWeights();
        when(placeStatsRepository.findAll()).thenReturn(List.of(stats));

        RankingRecalculationResult result = rankingRecalculationService.recalculate(null);

        ArgumentCaptor<Iterable<PlaceSeasonScore>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(placeSeasonScoreRepository).deleteBySeasonId(SEASON_ID);
        verify(placeSeasonScoreRepository).saveAll(captor.capture());
        List<PlaceSeasonScore> savedScores = new ArrayList<>();
        captor.getValue().forEach(savedScores::add);

        assertThat(result.placeCount()).isZero();
        assertThat(result.scoreCount()).isZero();
        assertThat(savedScores).isEmpty();
        verify(rankingCache).evictAllPlaceRankings();
    }

    private void stubActiveSeasonAndWeights() {
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));
        when(policyService.getRequiredDecimal("ranking", "recommend_weight")).thenReturn(BigDecimal.ONE);
        when(policyService.getRequiredDecimal("ranking", "visit_weight")).thenReturn(BigDecimal.valueOf(2));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));
    }
}
