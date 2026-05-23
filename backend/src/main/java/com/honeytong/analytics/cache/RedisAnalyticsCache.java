package com.honeytong.analytics.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.analytics.dto.AdminAnalyticsResponse;
import com.honeytong.analytics.dto.RegionalTrendsResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisAnalyticsCache implements AnalyticsCache {

    private static final String REGIONAL_KEY_PREFIX = "analytics:regional:";
    private static final String ADMIN_KEY = "analytics:admin:trends";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalyticsRedisProperties properties;

    public RedisAnalyticsCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AnalyticsRedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<RegionalTrendsResponse> getRegionalTrends(Long dongId) {
        try {
            String rawValue = redisTemplate.opsForValue().get(REGIONAL_KEY_PREFIX + dongId);
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            RegionalTrendsResponse trends = objectMapper.readValue(rawValue, RegionalTrendsResponse.class);
            return Optional.of(trends);
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void putRegionalTrends(Long dongId, RegionalTrendsResponse trends) {
        try {
            String rawValue = objectMapper.writeValueAsString(trends);
            redisTemplate.opsForValue().set(
                    REGIONAL_KEY_PREFIX + dongId,
                    rawValue,
                    properties.regionalTrendsTtl()
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            // DB is the source of truth if Redis is unavailable.
        }
    }

    @Override
    public void evictRegionalTrends(Long dongId) {
        try {
            redisTemplate.delete(REGIONAL_KEY_PREFIX + dongId);
        } catch (RuntimeException exception) {
            // Eviction failure must not break client workflows.
        }
    }

    @Override
    public Optional<AdminAnalyticsResponse> getAdminAnalytics() {
        try {
            String rawValue = redisTemplate.opsForValue().get(ADMIN_KEY);
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            AdminAnalyticsResponse trends = objectMapper.readValue(rawValue, AdminAnalyticsResponse.class);
            return Optional.of(trends);
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void putAdminAnalytics(AdminAnalyticsResponse trends) {
        try {
            String rawValue = objectMapper.writeValueAsString(trends);
            redisTemplate.opsForValue().set(
                    ADMIN_KEY,
                    rawValue,
                    properties.adminTrendsTtl()
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            // DB remains the source of truth if Redis is unavailable.
        }
    }

    @Override
    public void evictAdminAnalytics() {
        try {
            redisTemplate.delete(ADMIN_KEY);
        } catch (RuntimeException exception) {
            // Eviction failure must not break client workflows.
        }
    }
}
