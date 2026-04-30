package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.ranking.dto.AdminRankingRecalculateRequest;
import com.honeytong.ranking.dto.AdminSeasonCreateRequest;
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
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
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
class AdminRankingServiceTest {

    private static final long ADMIN_ID = 1L;
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
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    private AdminRankingService adminRankingService;
    private User admin;
    private Season season;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        adminRankingService = new AdminRankingService(
                seasonRepository,
                placeStatsRepository,
                placeSeasonScoreRepository,
                policyService,
                userRepository,
                adminActionLogRepository,
                new ObjectMapper()
        );

        admin = new User("관리자", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        season = new Season(
                "2026-04",
                "2026년 4월",
                SeasonType.MONTHLY,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                SeasonStatus.ACTIVE
        );
        ReflectionTestUtils.setField(season, "id", SEASON_ID);

        RegionCity city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 20L);
        RegionDistrict district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 30L);
        RegionDong dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 40L);

        User owner = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(owner, "id", 2L);
        place = new Place(
                owner,
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

        stats = new PlaceStats(place);
        stats.addRecommendation(BigDecimal.ONE);
        stats.addVisit(BigDecimal.valueOf(2));
        stats.addComment(BigDecimal.valueOf(0.5));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculatePlaceRankings_writesScoresFromAggregatedStatsAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));
        when(placeStatsRepository.findAll()).thenReturn(List.of(stats));
        when(policyService.getRequiredDecimal("ranking", "recommend_weight")).thenReturn(BigDecimal.ONE);
        when(policyService.getRequiredDecimal("ranking", "visit_weight")).thenReturn(BigDecimal.valueOf(2));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = adminRankingService.recalculatePlaceRankings(
                ADMIN_ID,
                new AdminRankingRecalculateRequest(null, "테스트 재계산")
        );

        ArgumentCaptor<Iterable<PlaceSeasonScore>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(placeSeasonScoreRepository).deleteBySeasonId(SEASON_ID);
        verify(placeSeasonScoreRepository).saveAll(captor.capture());
        List<PlaceSeasonScore> savedScores = new ArrayList<>();
        captor.getValue().forEach(savedScores::add);

        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.placeCount()).isEqualTo(1);
        assertThat(response.scoreCount()).isEqualTo(3);
        assertThat(savedScores).hasSize(3);
        assertThat(savedScores)
                .extracting(PlaceSeasonScore::getRegionType)
                .containsExactlyInAnyOrder(RankingRegionType.DONG, RankingRegionType.DISTRICT, RankingRegionType.CITY);
        assertThat(place.getCurrentStarLevel()).isEqualTo(3);
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void createSeason_rejectsDuplicateSeasonCode() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(seasonRepository.existsBySeasonCode("2026-04")).thenReturn(true);

        assertThatThrownBy(() -> adminRankingService.createSeason(
                ADMIN_ID,
                new AdminSeasonCreateRequest(
                        "2026-04",
                        "2026년 4월",
                        SeasonType.MONTHLY,
                        LocalDateTime.of(2026, 4, 1, 0, 0),
                        LocalDateTime.of(2026, 4, 30, 23, 59),
                        SeasonStatus.PLANNED,
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createSeason_rejectsSecondActiveSeason() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(seasonRepository.existsBySeasonCode("2026-05")).thenReturn(false);
        when(seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE))
                .thenReturn(Optional.of(season));

        assertThatThrownBy(() -> adminRankingService.createSeason(
                ADMIN_ID,
                new AdminSeasonCreateRequest(
                        "2026-05",
                        "2026년 5월",
                        SeasonType.MONTHLY,
                        LocalDateTime.of(2026, 5, 1, 0, 0),
                        LocalDateTime.of(2026, 5, 31, 23, 59),
                        SeasonStatus.ACTIVE,
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
