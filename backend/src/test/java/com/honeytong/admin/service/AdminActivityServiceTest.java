package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminActivityInvalidationRequest;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.recommendation.counter.RecommendationDailyCounter;
import com.honeytong.recommendation.entity.Recommendation;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.visit.cooldown.VisitCooldownCache;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.repository.VisitRepository;
import com.honeytong.place.service.PlaceAudienceStatsService;
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
class AdminActivityServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long PLACE_ID = 100L;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private RecommendationDailyCounter recommendationDailyCounter;

    @Mock
    private VisitCooldownCache visitCooldownCache;

    @Mock
    private PlaceAudienceStatsService placeAudienceStatsService;

    private AdminActivityService adminActivityService;
    private User admin;
    private User user;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        adminActivityService = new AdminActivityService(
                recommendationRepository,
                visitRepository,
                placeStatsRepository,
                userRepository,
                adminActionLogRepository,
                policyService,
                recommendationDailyCounter,
                visitCooldownCache,
                new ObjectMapper(),
                placeAudienceStatsService
        );

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        user = new User("bee", "bee@example.com");
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
                "Road address",
                "Jibun address",
                BigDecimal.valueOf(37.55),
                BigDecimal.valueOf(126.91),
                "10000_20000",
                "Soup",
                "Worth revisiting.",
                "Deep broth.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);
    }

    @Test
    void getRecommendations_returnsLatestRecommendationLogs() {
        Recommendation recommendation = new Recommendation(user, place, BigDecimal.valueOf(1.25));
        ReflectionTestUtils.setField(recommendation, "id", 501L);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(recommendationRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(recommendation));

        var response = adminActivityService.getRecommendations(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().recommendationId()).isEqualTo(501L);
        assertThat(response.getFirst().userId()).isEqualTo(USER_ID);
        assertThat(response.getFirst().nickname()).isEqualTo("bee");
        assertThat(response.getFirst().placeId()).isEqualTo(PLACE_ID);
        assertThat(response.getFirst().placeName()).isEqualTo("Local Soup");
        assertThat(response.getFirst().categoryCode()).isEqualTo("KOREAN");
        assertThat(response.getFirst().status()).isEqualTo(RecommendationStatus.ACTIVE);
        assertThat(response.getFirst().recommendWeight()).isEqualByComparingTo("1.25");
    }

    @Test
    void getVisits_returnsLatestVisitLogs() {
        Visit visit = new Visit(
                user,
                place,
                BigDecimal.valueOf(37.550001),
                BigDecimal.valueOf(126.910001),
                42,
                "https://image.example.com/visit.jpg",
                true,
                "WITHIN_RADIUS"
        );
        ReflectionTestUtils.setField(visit, "id", 701L);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(visitRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(visit));

        var response = adminActivityService.getVisits(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().visitId()).isEqualTo(701L);
        assertThat(response.getFirst().userId()).isEqualTo(USER_ID);
        assertThat(response.getFirst().nickname()).isEqualTo("bee");
        assertThat(response.getFirst().placeId()).isEqualTo(PLACE_ID);
        assertThat(response.getFirst().placeName()).isEqualTo("Local Soup");
        assertThat(response.getFirst().distanceMeter()).isEqualTo(42);
        assertThat(response.getFirst().imageUrl()).isEqualTo("https://image.example.com/visit.jpg");
        assertThat(response.getFirst().valid()).isTrue();
        assertThat(response.getFirst().validReason()).isEqualTo("WITHIN_RADIUS");
    }

    @Test
    void getVisits_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminActivityService.getVisits(9L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void invalidateRecommendation_changesStatusSubtractsStatsAndLogsAction() {
        Recommendation recommendation = new Recommendation(user, place, BigDecimal.valueOf(1.25));
        ReflectionTestUtils.setField(recommendation, "id", 501L);
        stats.addRecommendation(BigDecimal.valueOf(1.25));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(recommendationRepository.findById(501L)).thenReturn(Optional.of(recommendation));
        when(placeStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminActivityService.invalidateRecommendation(
                ADMIN_ID,
                501L,
                new AdminActivityInvalidationRequest("Abuse confirmed.")
        );

        assertThat(response.recommendationId()).isEqualTo(501L);
        assertThat(response.status()).isEqualTo(RecommendationStatus.INVALIDATED);
        assertThat(response.recommendCount()).isZero();
        assertThat(stats.getRecommendCount()).isZero();
        assertThat(stats.getScoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(recommendationDailyCounter).evict(org.mockito.Mockito.eq(USER_ID), org.mockito.Mockito.any());
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void invalidateRecommendation_doesNotLogWhenAlreadyInactive() {
        Recommendation recommendation = new Recommendation(user, place, BigDecimal.valueOf(1.25));
        ReflectionTestUtils.setField(recommendation, "id", 501L);
        recommendation.cancel();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(recommendationRepository.findById(501L)).thenReturn(Optional.of(recommendation));
        when(placeStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminActivityService.invalidateRecommendation(
                ADMIN_ID,
                501L,
                new AdminActivityInvalidationRequest("No-op.")
        );

        assertThat(response.status()).isEqualTo(RecommendationStatus.CANCELED);
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void invalidateVisit_changesValiditySubtractsStatsAndLogsAction() {
        Visit visit = new Visit(
                user,
                place,
                BigDecimal.valueOf(37.550001),
                BigDecimal.valueOf(126.910001),
                42,
                "https://image.example.com/visit.jpg",
                true,
                "VALID"
        );
        ReflectionTestUtils.setField(visit, "id", 701L);
        stats.addVisit(BigDecimal.valueOf(2));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(visitRepository.findById(701L)).thenReturn(Optional.of(visit));
        when(placeStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "visit_weight")).thenReturn(BigDecimal.valueOf(2));

        var response = adminActivityService.invalidateVisit(
                ADMIN_ID,
                701L,
                new AdminActivityInvalidationRequest("GPS review failed.")
        );

        assertThat(response.visitId()).isEqualTo(701L);
        assertThat(response.valid()).isFalse();
        assertThat(response.validReason()).isEqualTo("ADMIN_INVALIDATED");
        assertThat(response.visitCount()).isZero();
        assertThat(stats.getVisitCount()).isZero();
        assertThat(stats.getScoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(visitCooldownCache).evict(USER_ID, PLACE_ID);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void invalidateVisit_doesNotLogWhenAlreadyInvalid() {
        Visit visit = new Visit(
                user,
                place,
                BigDecimal.valueOf(37.550001),
                BigDecimal.valueOf(126.910001),
                42,
                null,
                false,
                "ADMIN_INVALIDATED"
        );
        ReflectionTestUtils.setField(visit, "id", 701L);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(visitRepository.findById(701L)).thenReturn(Optional.of(visit));
        when(placeStatsRepository.findByIdForUpdate(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminActivityService.invalidateVisit(
                ADMIN_ID,
                701L,
                new AdminActivityInvalidationRequest("No-op.")
        );

        assertThat(response.valid()).isFalse();
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }
}
