package com.honeytong.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.honeytong.analytics.cache.AnalyticsCache;
import com.honeytong.analytics.dto.RegionalTrendsResponse;
import com.honeytong.analytics.dto.UserAnalyticsResponse;
import com.honeytong.analytics.repository.AnalyticsRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final long USER_ID = 1L;
    private static final long DONG_ID = 100L;

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalyticsCache analyticsCache;

    private AnalyticsService analyticsService;
    private User user;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository, userRepository, analyticsCache);
        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void getUserAnalytics_throwsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getUserAnalytics(USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void getUserAnalytics_throwsForbiddenWhenUserIsInactive() {
        ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.now()); // user.isActive() -> false
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> analyticsService.getUserAnalytics(USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getUserAnalytics_returnsCorrectAggregatesAndTrends() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        
        // Mock Category preferences: KOREAN (3 counts), CHINESE (1 count)
        when(analyticsRepository.findCategoryPreferences(USER_ID)).thenReturn(List.of(
                new Object[]{"KOREAN", 3L},
                new Object[]{"CHINESE", 1L}
        ));

        // Mock Price preferences: 10000_20000 (4 counts)
        when(analyticsRepository.findPricePreferences(USER_ID)).thenReturn(List.<Object[]>of(
                new Object[]{"10000_20000", 4L}
        ));

        // Mock daily trends: User had 1 recommendation and 1 visit today
        LocalDate today = LocalDate.now();
        when(analyticsRepository.findUserRecommendationTrends(eq(USER_ID), any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(today), 1L}
        ));
        when(analyticsRepository.findUserVisitTrends(eq(USER_ID), any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(today), 1L}
        ));
        when(analyticsRepository.findUserCommentTrends(eq(USER_ID), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        UserAnalyticsResponse response = analyticsService.getUserAnalytics(USER_ID);

        // Check Category: KOREAN should be 75%, CHINESE should be 25%
        assertThat(response.categoryPreferences()).hasSize(2);
        assertThat(response.categoryPreferences().get(0).categoryCode()).isEqualTo("KOREAN");
        assertThat(response.categoryPreferences().get(0).percentage()).isEqualTo(75.0);
        assertThat(response.categoryPreferences().get(1).categoryCode()).isEqualTo("CHINESE");
        assertThat(response.categoryPreferences().get(1).percentage()).isEqualTo(25.0);

        // Check Price: 10000_20000 should be 100%
        assertThat(response.priceRangePreferences()).hasSize(1);
        assertThat(response.priceRangePreferences().get(0).percentage()).isEqualTo(100.0);

        // Check activity trend padding: Should contain 30 elements
        assertThat(response.recentActivityTrends()).hasSize(30);
        // The last element (today) should have counts 1, 1, 0
        UserAnalyticsResponse.ActivityTrend todayTrend = response.recentActivityTrends().get(29);
        assertThat(todayTrend.date()).isEqualTo(today);
        assertThat(todayTrend.recommendationsCount()).isEqualTo(1L);
        assertThat(todayTrend.visitsCount()).isEqualTo(1L);
        assertThat(todayTrend.commentsCount()).isZero();
    }

    @Test
    void getRegionalTrends_returnsCachedDataIfPresent() {
        RegionalTrendsResponse cachedResponse = new RegionalTrendsResponse(
                List.of(new RegionalTrendsResponse.TrendingPlace(5L, "맛집", 10L)),
                List.of(new RegionalTrendsResponse.RisingCategory("KOREAN", 12L))
        );
        when(analyticsCache.getRegionalTrends(DONG_ID)).thenReturn(Optional.of(cachedResponse));

        RegionalTrendsResponse result = analyticsService.getRegionalTrends(DONG_ID);

        assertThat(result).isEqualTo(cachedResponse);
        verify(analyticsRepository, never()).findTrendingPlaces(any(), any(), anyInt());
    }

    @Test
    void getRegionalTrends_queriesDbAndCachesOnMiss() {
        when(analyticsCache.getRegionalTrends(DONG_ID)).thenReturn(Optional.empty());

        when(analyticsRepository.findTrendingPlaces(eq(DONG_ID), any(LocalDateTime.class), eq(10))).thenReturn(List.<Object[]>of(
                new Object[]{5L, "국밥집", 8L}
        ));
        when(analyticsRepository.findRisingCategories(eq(DONG_ID), any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{"KOREAN", 10L}
        ));

        RegionalTrendsResponse result = analyticsService.getRegionalTrends(DONG_ID);

        assertThat(result.trendingPlaces()).hasSize(1);
        assertThat(result.trendingPlaces().get(0).placeName()).isEqualTo("국밥집");
        assertThat(result.trendingPlaces().get(0).activityCount()).isEqualTo(8L);

        assertThat(result.risingCategories()).hasSize(1);
        assertThat(result.risingCategories().get(0).categoryCode()).isEqualTo("KOREAN");

        verify(analyticsCache).putRegionalTrends(eq(DONG_ID), any(RegionalTrendsResponse.class));
    }
}
