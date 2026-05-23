package com.honeytong.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.honeytong.analytics.cache.AnalyticsCache;
import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.repository.AnalyticsRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
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
class AdminAnalyticsServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long NORMAL_USER_ID = 2L;

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalyticsCache analyticsCache;

    private AdminAnalyticsService adminAnalyticsService;
    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminAnalyticsService = new AdminAnalyticsService(analyticsRepository, userRepository, analyticsCache);

        admin = new User("어드민", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        normalUser = new User("일반유저", "user@example.com");
        ReflectionTestUtils.setField(normalUser, "id", NORMAL_USER_ID);
        ReflectionTestUtils.setField(normalUser, "role", UserRole.USER);
    }

    @Test
    void getGlobalActivityTrends_throwsForbiddenWhenNotAdmin() {
        when(userRepository.findById(NORMAL_USER_ID)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminAnalyticsService.getGlobalActivityTrends(NORMAL_USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getGlobalActivityTrends_returnsCachedDataIfPresent() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        AdminAnalyticsResponse cachedResponse = new AdminAnalyticsResponse(
                List.of(new AdminAnalyticsResponse.DailyActivityTrend(LocalDate.now(), 2L, 1L, 5L, 3L))
        );
        when(analyticsCache.getAdminAnalytics()).thenReturn(Optional.of(cachedResponse));

        AdminAnalyticsResponse result = adminAnalyticsService.getGlobalActivityTrends(ADMIN_ID);

        assertThat(result).isEqualTo(cachedResponse);
        verify(analyticsRepository, never()).findDailyNewUsers(any());
    }

    @Test
    void getGlobalActivityTrends_queriesDbAndCachesOnMiss() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(analyticsCache.getAdminAnalytics()).thenReturn(Optional.empty());

        LocalDate today = LocalDate.now();

        when(analyticsRepository.findDailyNewUsers(any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(today), 5L}
        ));
        when(analyticsRepository.findDailyNewPlaces(any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(today), 2L}
        ));
        when(analyticsRepository.findDailyRecommendations(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(analyticsRepository.findDailyVisits(any(LocalDateTime.class))).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(today), 10L}
        ));

        AdminAnalyticsResponse result = adminAnalyticsService.getGlobalActivityTrends(ADMIN_ID);

        assertThat(result.dailyTrends()).hasSize(30);

        AdminAnalyticsResponse.DailyActivityTrend todayTrend = result.dailyTrends().get(29);
        assertThat(todayTrend.date()).isEqualTo(today);
        assertThat(todayTrend.newUsersCount()).isEqualTo(5L);
        assertThat(todayTrend.newPlacesCount()).isEqualTo(2L);
        assertThat(todayTrend.recommendationsCount()).isZero();
        assertThat(todayTrend.visitsCount()).isEqualTo(10L);

        verify(analyticsCache).putAdminAnalytics(any(AdminAnalyticsResponse.class));
    }
}
