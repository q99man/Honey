package com.honeytong.visit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import com.honeytong.user.service.UserGrowthService;
import com.honeytong.user.service.VisitGrowthResult;
import com.honeytong.visit.cooldown.VisitCooldownCache;
import com.honeytong.visit.dto.VisitVerifyRequest;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.repository.VisitRepository;
import com.honeytong.place.service.PlaceAudienceStatsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class VisitServiceTest {

    private static final long USER_ID = 1L;
    private static final long PLACE_ID = 100L;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private UserGrowthService userGrowthService;

    @Mock
    private UserActionLogService userActionLogService;

    @Mock
    private VisitCooldownCache visitCooldownCache;

    @Mock
    private PlaceAudienceStatsService placeAudienceStatsService;

    private VisitService visitService;
    private User user;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(
                visitRepository,
                placeRepository,
                placeStatsRepository,
                userRepository,
                policyService,
                userGrowthService,
                userActionLogService,
                visitCooldownCache,
                placeAudienceStatsService
        );

        user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);
        place = new Place(
                user,
                dong,
                "Local Soup",
                "KOREAN",
                "Seoul Mapo-gu Yanghwa-ro 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Soup",
                "A neighborhood soup place worth revisiting.",
                "Clear broth and fast turnover.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);

        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredInteger("visit", "image_url_max_length"))
                .thenReturn(255);
    }

    @Test
    void verifyVisit_savesValidVisitAndUpdatesStats() {
        stubActiveUserAndVisiblePlace();
        when(policyService.getRequiredInteger("visit", "radius_meter")).thenReturn(70);
        when(policyService.getRequiredInteger("visit", "cooldown_hour")).thenReturn(24);
        when(visitCooldownCache.getCooldownUntil(eq(USER_ID), eq(PLACE_ID), any()))
                .thenReturn(Optional.empty());
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(placeStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "visit_weight")).thenReturn(BigDecimal.valueOf(2.0));
        when(userGrowthService.applyValidVisit(USER_ID)).thenReturn(new VisitGrowthResult(2, 1));

        var response = visitService.verifyVisit(
                USER_ID,
                PLACE_ID,
                new VisitVerifyRequest(
                        BigDecimal.valueOf(37.5501000),
                        BigDecimal.valueOf(126.9101000),
                        " https://image.example.com/visit.jpg "
                )
        );

        assertThat(response.verified()).isTrue();
        assertThat(response.distanceMeter()).isLessThanOrEqualTo(70);
        assertThat(response.expGained()).isEqualTo(2);
        assertThat(response.visitCount()).isEqualTo(1);

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(visitRepository).save(visitCaptor.capture());
        verify(visitCooldownCache).evict(USER_ID, PLACE_ID);
        verify(userGrowthService).applyValidVisit(USER_ID);
        verify(userActionLogService).record(
                org.mockito.Mockito.eq(USER_ID),
                org.mockito.Mockito.eq(UserActionLogService.ACTION_VISIT_VERIFY),
                org.mockito.Mockito.eq(UserActionLogService.TARGET_PLACE),
                org.mockito.Mockito.eq(PLACE_ID),
                org.mockito.Mockito.any()
        );
        assertThat(visitCaptor.getValue().isValid()).isTrue();
        assertThat(visitCaptor.getValue().getDistanceMeter()).isEqualTo(response.distanceMeter());
    }

    @Test
    void verifyVisit_rejectsOutOfRadius() {
        stubActiveUserAndVisiblePlace();
        when(policyService.getRequiredInteger("visit", "radius_meter")).thenReturn(70);
        when(policyService.getRequiredInteger("visit", "cooldown_hour")).thenReturn(24);
        when(visitCooldownCache.getCooldownUntil(eq(USER_ID), eq(PLACE_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitService.verifyVisit(
                USER_ID,
                PLACE_ID,
                new VisitVerifyRequest(
                        BigDecimal.valueOf(37.5700000),
                        BigDecimal.valueOf(126.9300000),
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_VISIT_RADIUS));
        verify(visitRepository, never()).save(any(Visit.class));
        verify(placeStatsRepository, never()).findById(any());
    }

    @Test
    void verifyVisit_rejectsActiveCooldown() {
        stubActiveUserAndVisiblePlace();
        when(policyService.getRequiredInteger("visit", "radius_meter")).thenReturn(70);
        when(policyService.getRequiredInteger("visit", "cooldown_hour")).thenReturn(24);
        when(visitCooldownCache.getCooldownUntil(eq(USER_ID), eq(PLACE_ID), any()))
                .thenReturn(Optional.of(LocalDateTime.now().plusHours(23)));

        assertThatThrownBy(() -> visitService.verifyVisit(
                USER_ID,
                PLACE_ID,
                new VisitVerifyRequest(
                        BigDecimal.valueOf(37.5501000),
                        BigDecimal.valueOf(126.9101000),
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VISIT_COOLDOWN_ACTIVE));
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @Test
    void verifyVisit_rejectsImageUrlLongerThanPolicyLimit() {
        stubActiveUserAndVisiblePlace();
        when(policyService.getRequiredInteger("visit", "radius_meter")).thenReturn(70);
        when(policyService.getRequiredInteger("visit", "cooldown_hour")).thenReturn(24);
        when(policyService.getRequiredInteger("visit", "image_url_max_length")).thenReturn(5);
        when(visitCooldownCache.getCooldownUntil(eq(USER_ID), eq(PLACE_ID), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> visitService.verifyVisit(
                USER_ID,
                PLACE_ID,
                new VisitVerifyRequest(
                        BigDecimal.valueOf(37.5501000),
                        BigDecimal.valueOf(126.9101000),
                        "123456"
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(visitRepository, never()).save(any(Visit.class));
        verify(placeStatsRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void getVisitPolicy_returnsCooldownUntil() {
        LocalDateTime recentAt = LocalDateTime.now().minusHours(1);
        stubActiveUserAndVisiblePlace();
        when(policyService.getRequiredInteger("visit", "radius_meter")).thenReturn(70);
        when(policyService.getRequiredInteger("visit", "cooldown_hour")).thenReturn(24);
        when(visitCooldownCache.getCooldownUntil(eq(USER_ID), eq(PLACE_ID), any()))
                .thenReturn(Optional.of(recentAt.plusHours(24)));

        var response = visitService.getVisitPolicy(USER_ID, PLACE_ID);

        assertThat(response.canVisitNow()).isFalse();
        assertThat(response.radiusMeter()).isEqualTo(70);
        assertThat(response.cooldownUntil()).isEqualTo(recentAt.plusHours(24));
    }

    @Test
    void getMyVisits_returnsValidVisits() {
        LocalDateTime visitedAt = LocalDateTime.now().minusHours(2);
        Visit visit = recentVisit(visitedAt);
        ReflectionTestUtils.setField(visit, "id", 200L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(visitRepository.findByUserIdAndValidTrueOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(visit));

        var response = visitService.getMyVisits(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).visitId()).isEqualTo(200L);
        assertThat(response.get(0).placeId()).isEqualTo(PLACE_ID);
        assertThat(response.get(0).placeName()).isEqualTo("Local Soup");
        assertThat(response.get(0).visitedAt()).isEqualTo(visitedAt);
    }

    @Test
    void getPlaceVisitSummary_returnsStatsAndLastVisitTime() {
        LocalDateTime visitedAt = LocalDateTime.now().minusMinutes(30);
        stats.addVisit(BigDecimal.valueOf(2.0));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(visitRepository.findTopByPlaceIdAndValidTrueOrderByCreatedAtDesc(PLACE_ID))
                .thenReturn(Optional.of(recentVisit(visitedAt)));

        var response = visitService.getPlaceVisitSummary(PLACE_ID);

        assertThat(response.placeId()).isEqualTo(PLACE_ID);
        assertThat(response.visitCount()).isEqualTo(1);
        assertThat(response.lastVisitedAt()).isEqualTo(visitedAt);
    }

    private void stubActiveUserAndVisiblePlace() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
    }

    private Visit recentVisit(LocalDateTime createdAt) {
        Visit visit = new Visit(
                user,
                place,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                0,
                null,
                true,
                "VALID"
        );
        ReflectionTestUtils.setField(visit, "createdAt", createdAt);
        return visit;
    }
}
