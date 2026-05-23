package com.honeytong.analytics.service;

import com.honeytong.analytics.cache.AnalyticsCache;
import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.repository.AnalyticsRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final AnalyticsCache analyticsCache;

    public AdminAnalyticsService(
            AnalyticsRepository analyticsRepository,
            UserRepository userRepository,
            AnalyticsCache analyticsCache
    ) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.analyticsCache = analyticsCache;
    }

    public AdminAnalyticsResponse getGlobalActivityTrends(Long adminUserId) {
        ensureAdmin(adminUserId);

        Optional<AdminAnalyticsResponse> cached = analyticsCache.getAdminAnalytics();
        if (cached.isPresent()) {
            return cached.get();
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(29).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Map<LocalDate, Long> newUsersMap = toDateCountMap(analyticsRepository.findDailyNewUsers(startDate));
        Map<LocalDate, Long> newPlacesMap = toDateCountMap(analyticsRepository.findDailyNewPlaces(startDate));
        Map<LocalDate, Long> recommendationsMap = toDateCountMap(analyticsRepository.findDailyRecommendations(startDate));
        Map<LocalDate, Long> visitsMap = toDateCountMap(analyticsRepository.findDailyVisits(startDate));

        List<AdminAnalyticsResponse.DailyActivityTrend> dailyTrends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            dailyTrends.add(new AdminAnalyticsResponse.DailyActivityTrend(
                    targetDate,
                    newUsersMap.getOrDefault(targetDate, 0L),
                    newPlacesMap.getOrDefault(targetDate, 0L),
                    recommendationsMap.getOrDefault(targetDate, 0L),
                    visitsMap.getOrDefault(targetDate, 0L)
            ));
        }

        AdminAnalyticsResponse response = new AdminAnalyticsResponse(dailyTrends);
        analyticsCache.putAdminAnalytics(response);
        return response;
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private Map<LocalDate, Long> toDateCountMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(toLocalDate(row[0]), toLong(row[1]));
        }
        return map;
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        } else if (obj instanceof java.time.LocalDate) {
            return (LocalDate) obj;
        } else if (obj instanceof String) {
            return LocalDate.parse((String) obj);
        } else if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported date type: " + obj.getClass());
    }

    private long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(String.valueOf(obj));
    }
}
