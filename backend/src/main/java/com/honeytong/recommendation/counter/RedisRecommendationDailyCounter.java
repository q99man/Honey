package com.honeytong.recommendation.counter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.function.LongSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisRecommendationDailyCounter implements RecommendationDailyCounter {

    private static final String KEY_PREFIX = "recommend:daily:user:";
    private static final Duration CACHE_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    public RedisRecommendationDailyCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long getUsedCount(Long userId, LocalDate date, LongSupplier databaseCountSupplier) {
        String cacheKey = key(userId, date);
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null && !cachedValue.isBlank()) {
                return Long.parseLong(cachedValue);
            }
        } catch (RuntimeException exception) {
            return databaseCountSupplier.getAsLong();
        }

        long databaseCount = databaseCountSupplier.getAsLong();
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(databaseCount), CACHE_TTL);
        } catch (RuntimeException exception) {
            // The recommendation rows remain the durable source of truth.
        }
        return databaseCount;
    }

    @Override
    public void evict(Long userId, LocalDate date) {
        try {
            redisTemplate.delete(key(userId, date));
        } catch (RuntimeException exception) {
            // A cache eviction failure must not break the completed recommendation workflow.
        }
    }

    private String key(Long userId, LocalDate date) {
        return KEY_PREFIX + userId + ":" + date.toString().replace("-", "");
    }
}
