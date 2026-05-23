package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private com.honeytong.recommendation.repository.RecommendationRepository recommendationRepository;

    @Mock
    private com.honeytong.visit.repository.VisitRepository visitRepository;

    @Mock
    private com.honeytong.comment.repository.CommentRepository commentRepository;

    @Mock
    private com.honeytong.place.repository.PlaceAudienceStatsRepository placeAudienceStatsRepository;

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
                rankingCache,
                recommendationRepository,
                visitRepository,
                commentRepository,
                placeAudienceStatsRepository
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
        ReflectionTestUtils.setField(stats, "placeId", PLACE_ID);
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

        when(recommendationRepository.countByPlaceIdAndStatusAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any(), any())).thenReturn(2L);
        when(visitRepository.countByPlaceIdAndValidTrueAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any())).thenReturn(3L);
        when(commentRepository.countByPlaceIdAndStatusAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any(), any())).thenReturn(1L);

        com.honeytong.place.entity.PlaceAudienceStats audienceStats = new com.honeytong.place.entity.PlaceAudienceStats(place);
        audienceStats.setAge20Count(4);
        audienceStats.setAge30Count(4);
        audienceStats.setMaleCount(5);
        audienceStats.setFemaleCount(5);
        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(audienceStats));

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
                .allSatisfy(totalScore -> assertThat(totalScore).isCloseTo(BigDecimal.valueOf(15.104), org.assertj.core.data.Offset.offset(BigDecimal.valueOf(0.002))));
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

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_updatesRecencyAndDiversityScoresBasedOnPolicies() {
        stubActiveSeasonAndWeights();
        when(placeStatsRepository.findAll()).thenReturn(List.of(stats));

        // 30 days counts: recommendation=10, visit=5, comment=10
        // Recency Score = 10 * 0.5 + 5 * 1.0 + 10 * 0.2 = 5.0 + 5.0 + 2.0 = 12.0
        when(recommendationRepository.countByPlaceIdAndStatusAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any(), any())).thenReturn(10L);
        when(visitRepository.countByPlaceIdAndValidTrueAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any())).thenReturn(5L);
        when(commentRepository.countByPlaceIdAndStatusAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                eq(PLACE_ID), any(), any())).thenReturn(10L);

        // Demographic diversity:
        // Age group counts: 10s: 0, 20s: 10, 30s: 10, 40s: 0, 50s+: 0 (total 20)
        // Gender group counts: Male: 10, Female: 10, Other: 0 (total 20)
        // Age entropy: -(0.5 * ln(0.5) + 0.5 * ln(0.5)) = ln(2) = 0.693147
        // Gender entropy: -(0.5 * ln(0.5) + 0.5 * ln(0.5)) = ln(2) = 0.693147
        // Normalized age: ln(2)/ln(5) = 0.693147 / 1.609438 = 0.430676
        // Normalized gender: ln(2)/ln(3) = 0.693147 / 1.098612 = 0.630930
        // Average diversity ratio = (0.430676 + 0.630930) / 2 = 0.530803
        // Diversity Score = 0.530803 * 5.0 = 2.654015
        com.honeytong.place.entity.PlaceAudienceStats audienceStats = new com.honeytong.place.entity.PlaceAudienceStats(place);
        audienceStats.setAge20Count(10);
        audienceStats.setAge30Count(10);
        audienceStats.setMaleCount(10);
        audienceStats.setFemaleCount(10);
        when(placeAudienceStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(audienceStats));

        RankingRecalculationResult result = rankingRecalculationService.recalculate(null);

        // Verify place stats update call
        verify(placeStatsRepository).save(stats);
        
        // Assert recentScore is roughly 12.0 and diversityScore is roughly 2.65
        assertThat(stats.getRecentScore()).isEqualByComparingTo("12.0");
        assertThat(stats.getDiversityScore()).isCloseTo(BigDecimal.valueOf(2.654015), org.assertj.core.data.Offset.offset(BigDecimal.valueOf(0.001)));
    }

    private void stubActiveSeasonAndWeights() {
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));
        when(policyService.getRequiredDecimal("ranking", "recommend_weight")).thenReturn(BigDecimal.ONE);
        when(policyService.getRequiredDecimal("ranking", "visit_weight")).thenReturn(BigDecimal.valueOf(2));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        when(policyService.getRequiredInteger("ranking", "recency_days")).thenReturn(30);
        when(policyService.getRequiredDecimal("ranking", "recency_recommend_weight")).thenReturn(BigDecimal.valueOf(0.5));
        when(policyService.getRequiredDecimal("ranking", "recency_visit_weight")).thenReturn(BigDecimal.ONE);
        when(policyService.getRequiredDecimal("ranking", "recency_comment_weight")).thenReturn(BigDecimal.valueOf(0.2));
        when(policyService.getRequiredDecimal("ranking", "diversity_weight")).thenReturn(BigDecimal.valueOf(5));
    }
}
