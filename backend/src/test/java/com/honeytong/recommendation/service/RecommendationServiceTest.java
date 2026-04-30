package com.honeytong.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.recommendation.entity.Recommendation;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final long USER_ID = 1L;
    private static final long PLACE_ID = 100L;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private PolicyService policyService;

    private RecommendationService recommendationService;
    private User user;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                recommendationRepository,
                placeRepository,
                placeStatsRepository,
                userRepository,
                userTrustRepository,
                policyService
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        RegionCity city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);
        place = new Place(
                user,
                dong,
                "합정 국밥",
                "KOREAN",
                "서울 마포구 양화로 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "국밥",
                "동네에서 다시 찾고 싶은 국밥집",
                "맑은 국물과 빠른 회전이 좋습니다.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);
    }

    @Test
    void recommend_createsRecommendationAndUpdatesStats() {
        UserTrust trust = new UserTrust(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(recommendationRepository.existsByUserIdAndPlaceIdAndStatus(
                USER_ID,
                PLACE_ID,
                RecommendationStatus.ACTIVE
        )).thenReturn(false);
        when(policyService.getRequiredInteger("recommend", "daily_limit")).thenReturn(20);
        when(recommendationRepository.countByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0L);
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(recommendationRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(Optional.empty());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = recommendationService.recommend(USER_ID, PLACE_ID);

        assertThat(response.recommended()).isTrue();
        assertThat(response.recommendCount()).isEqualTo(1);
        assertThat(response.myWeight()).isEqualByComparingTo("1.0");
        verify(recommendationRepository).save(any(Recommendation.class));
    }

    @Test
    void recommend_rejectsDuplicateActiveRecommendation() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(recommendationRepository.existsByUserIdAndPlaceIdAndStatus(
                USER_ID,
                PLACE_ID,
                RecommendationStatus.ACTIVE
        )).thenReturn(true);
        when(policyService.getRequiredInteger("recommend", "daily_limit")).thenReturn(20);

        assertThatThrownBy(() -> recommendationService.recommend(USER_ID, PLACE_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getRecommendationPolicy_blocksWhenDailyLimitExceeded() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(recommendationRepository.existsByUserIdAndPlaceIdAndStatus(
                USER_ID,
                PLACE_ID,
                RecommendationStatus.ACTIVE
        )).thenReturn(false);
        when(policyService.getRequiredInteger("recommend", "daily_limit")).thenReturn(1);
        when(recommendationRepository.countByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(1L);

        var response = recommendationService.getRecommendationPolicy(USER_ID, PLACE_ID);

        assertThat(response.canRecommend()).isFalse();
        assertThat(response.reason()).isEqualTo("DAILY_LIMIT_EXCEEDED");
        assertThat(response.dailyRemainingCount()).isZero();
    }

    @Test
    void cancelRecommendation_cancelsActiveRecommendationAndUpdatesStats() {
        Recommendation recommendation = new Recommendation(user, place, BigDecimal.ONE);
        stats.addRecommendation(BigDecimal.ONE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(recommendationRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID))
                .thenReturn(Optional.of(recommendation));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = recommendationService.cancelRecommendation(USER_ID, PLACE_ID);

        assertThat(response.recommended()).isFalse();
        assertThat(response.recommendCount()).isZero();
        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.CANCELED);
    }
}
