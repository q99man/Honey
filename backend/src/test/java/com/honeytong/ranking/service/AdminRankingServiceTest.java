package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.ranking.cache.RankingCache;
import com.honeytong.ranking.dto.AdminRankingHistoryFinalizeRequest;
import com.honeytong.ranking.dto.AdminRankingPlaceExclusionRequest;
import com.honeytong.ranking.dto.AdminRankingRecalculateRequest;
import com.honeytong.ranking.dto.AdminSeasonCreateRequest;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private RankingCache rankingCache;

    @Mock
    private RankingRecalculationService rankingRecalculationService;

    @Mock
    private RankingHistoryFinalizationService rankingHistoryFinalizationService;

    private AdminRankingService adminRankingService;
    private User admin;
    private Season season;
    private Place place;

    @BeforeEach
    void setUp() {
        adminRankingService = new AdminRankingService(
                seasonRepository,
                placeRepository,
                userRepository,
                adminActionLogRepository,
                rankingCache,
                rankingRecalculationService,
                rankingHistoryFinalizationService,
                new ObjectMapper()
        );

        admin = new User("admin", "admin@example.com");
        admin.promoteTo(UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);

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
    }

    @Test
    void recalculatePlaceRankings_delegatesToCoreServiceAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(rankingRecalculationService.recalculate(null))
                .thenReturn(new RankingRecalculationResult(SEASON_ID, "2026-04", 1, 3));

        var response = adminRankingService.recalculatePlaceRankings(
                ADMIN_ID,
                new AdminRankingRecalculateRequest(null, "Manual recalculation.")
        );

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(rankingRecalculationService).recalculate(null);
        verify(adminActionLogRepository).save(captor.capture());
        AdminActionLog log = captor.getValue();

        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.placeCount()).isEqualTo(1);
        assertThat(response.scoreCount()).isEqualTo(3);
        assertThat(log.getActionType()).isEqualTo("RANKING_RECALCULATE");
        assertThat(log.getTargetType()).isEqualTo("RANKING");
        assertThat(log.getTargetId()).isEqualTo(SEASON_ID);
        assertThat(log.getAfterValue()).contains("\"seasonCode\":\"2026-04\"");
        assertThat(log.getMemo()).isEqualTo("Manual recalculation.");
    }

    @Test
    void finalizeRankingHistory_delegatesToCoreServiceAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(rankingHistoryFinalizationService.finalizeSeasonHistory(SEASON_ID))
                .thenReturn(new RankingHistoryFinalizationResult(SEASON_ID, "2026-04", 3));

        var response = adminRankingService.finalizeRankingHistory(
                ADMIN_ID,
                SEASON_ID,
                new AdminRankingHistoryFinalizeRequest("Finalize April history.")
        );

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(rankingHistoryFinalizationService).finalizeSeasonHistory(SEASON_ID);
        verify(adminActionLogRepository).save(captor.capture());
        AdminActionLog log = captor.getValue();

        assertThat(response.seasonId()).isEqualTo(SEASON_ID);
        assertThat(response.seasonCode()).isEqualTo("2026-04");
        assertThat(response.historyCount()).isEqualTo(3);
        assertThat(log.getActionType()).isEqualTo("RANKING_HISTORY_FINALIZE");
        assertThat(log.getTargetType()).isEqualTo("SEASON");
        assertThat(log.getTargetId()).isEqualTo(SEASON_ID);
        assertThat(log.getAfterValue()).contains("\"historyCount\":3");
        assertThat(log.getMemo()).isEqualTo("Finalize April history.");
    }

    @Test
    void changePlaceRankingExclusion_excludesPlaceClearsStarAndLogsAction() {
        place.updateCurrentStarLevel(3);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminRankingService.changePlaceRankingExclusion(
                ADMIN_ID,
                PLACE_ID,
                new AdminRankingPlaceExclusionRequest(true, "Exclude from current ranking.")
        );

        assertThat(response.placeId()).isEqualTo(PLACE_ID);
        assertThat(response.rankingExcluded()).isTrue();
        assertThat(response.starLevel()).isZero();
        assertThat(place.isRankingExcluded()).isTrue();
        assertThat(place.getCurrentStarLevel()).isZero();
        verify(rankingCache).evictAllPlaceRankings();
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void changePlaceRankingExclusion_doesNotLogWhenStateIsUnchanged() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminRankingService.changePlaceRankingExclusion(
                ADMIN_ID,
                PLACE_ID,
                new AdminRankingPlaceExclusionRequest(false, "No-op.")
        );

        assertThat(response.rankingExcluded()).isFalse();
        verify(rankingCache, never()).evictAllPlaceRankings();
        verify(adminActionLogRepository, never()).save(any(AdminActionLog.class));
    }

    @Test
    void createSeason_rejectsDuplicateSeasonCode() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(seasonRepository.existsBySeasonCode("2026-04")).thenReturn(true);

        assertThatThrownBy(() -> adminRankingService.createSeason(
                ADMIN_ID,
                new AdminSeasonCreateRequest(
                        "2026-04",
                        "April 2026",
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
                        "May 2026",
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
