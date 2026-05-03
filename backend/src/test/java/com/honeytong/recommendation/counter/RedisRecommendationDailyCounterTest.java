package com.honeytong.recommendation.counter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRecommendationDailyCounterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 5, 2);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRecommendationDailyCounter counter;

    @BeforeEach
    void setUp() {
        counter = new RedisRecommendationDailyCounter(redisTemplate);
    }

    @Test
    void getUsedCount_returnsCachedCountWithoutDatabaseLookup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("recommend:daily:user:7:20260502")).thenReturn("4");
        AtomicInteger databaseCalls = new AtomicInteger();

        long usedCount = counter.getUsedCount(7L, DATE, () -> {
            databaseCalls.incrementAndGet();
            return 9L;
        });

        assertThat(usedCount).isEqualTo(4L);
        assertThat(databaseCalls).hasValue(0);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void getUsedCount_cachesDatabaseCountWhenCacheMisses() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("recommend:daily:user:7:20260502")).thenReturn(null);

        long usedCount = counter.getUsedCount(7L, DATE, () -> 2L);

        assertThat(usedCount).isEqualTo(2L);
        verify(valueOperations).set(eq("recommend:daily:user:7:20260502"), eq("2"), any(Duration.class));
    }

    @Test
    void evict_deletesDailyCounterKey() {
        counter.evict(7L, DATE);

        verify(redisTemplate).delete("recommend:daily:user:7:20260502");
    }
}
