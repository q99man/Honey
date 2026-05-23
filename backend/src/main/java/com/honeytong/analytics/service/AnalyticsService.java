package com.honeytong.analytics.service;

import com.honeytong.analytics.cache.AnalyticsCache;
import com.honeytong.analytics.dto.RegionalTrendsResponse;
import com.honeytong.analytics.dto.UserAnalyticsResponse;
import com.honeytong.analytics.repository.AnalyticsRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final AnalyticsCache analyticsCache;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            UserRepository userRepository,
            AnalyticsCache analyticsCache
    ) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.analyticsCache = analyticsCache;
    }

    public UserAnalyticsResponse getUserAnalytics(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }

        // Category preferences
        List<Object[]> rawCategories = analyticsRepository.findCategoryPreferences(userId);
        long totalCategoryCount = rawCategories.stream().mapToLong(row -> toLong(row[1])).sum();
        List<UserAnalyticsResponse.CategoryPreference> categoryPreferences = rawCategories.stream()
                .map(row -> new UserAnalyticsResponse.CategoryPreference(
                        String.valueOf(row[0]),
                        totalCategoryCount == 0 ? 0.0 : Math.round((toLong(row[1]) * 100.0 / totalCategoryCount) * 10.0) / 10.0
                )).toList();

        // Price preferences
        List<Object[]> rawPrices = analyticsRepository.findPricePreferences(userId);
        long totalPriceCount = rawPrices.stream().mapToLong(row -> toLong(row[1])).sum();
        List<UserAnalyticsResponse.PricePreference> pricePreferences = rawPrices.stream()
                .map(row -> new UserAnalyticsResponse.PricePreference(
                        String.valueOf(row[0]),
                        totalPriceCount == 0 ? 0.0 : Math.round((toLong(row[1]) * 100.0 / totalPriceCount) * 10.0) / 10.0
                )).toList();

        // Activity trends (last 30 days)
        LocalDateTime startDate = LocalDateTime.now().minusDays(29).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Map<LocalDate, Long> recommendationsMap = toDateCountMap(analyticsRepository.findUserRecommendationTrends(userId, startDate));
        Map<LocalDate, Long> visitsMap = toDateCountMap(analyticsRepository.findUserVisitTrends(userId, startDate));
        Map<LocalDate, Long> commentsMap = toDateCountMap(analyticsRepository.findUserCommentTrends(userId, startDate));

        List<UserAnalyticsResponse.ActivityTrend> recentActivityTrends = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            recentActivityTrends.add(new UserAnalyticsResponse.ActivityTrend(
                    targetDate,
                    recommendationsMap.getOrDefault(targetDate, 0L),
                    visitsMap.getOrDefault(targetDate, 0L),
                    commentsMap.getOrDefault(targetDate, 0L)
            ));
        }

        return new UserAnalyticsResponse(categoryPreferences, pricePreferences, recentActivityTrends);
    }

    public RegionalTrendsResponse getRegionalTrends(Long dongId) {
        Optional<RegionalTrendsResponse> cached = analyticsCache.getRegionalTrends(dongId);
        if (cached.isPresent()) {
            return cached.get();
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(29).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        List<Object[]> rawTrendingPlaces = analyticsRepository.findTrendingPlaces(dongId, startDate, 10);
        List<RegionalTrendsResponse.TrendingPlace> trendingPlaces = rawTrendingPlaces.stream()
                .map(row -> new RegionalTrendsResponse.TrendingPlace(
                        toLong(row[0]),
                        String.valueOf(row[1]),
                        toLong(row[2])
                )).toList();

        List<Object[]> rawRisingCategories = analyticsRepository.findRisingCategories(dongId, startDate);
        List<RegionalTrendsResponse.RisingCategory> risingCategories = rawRisingCategories.stream()
                .map(row -> new RegionalTrendsResponse.RisingCategory(
                        String.valueOf(row[0]),
                        toLong(row[1])
                )).toList();

        RegionalTrendsResponse trends = new RegionalTrendsResponse(trendingPlaces, risingCategories);
        analyticsCache.putRegionalTrends(dongId, trends);
        return trends;
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
