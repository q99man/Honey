package com.honeytong.ranking.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.ranking.dto.PlaceRankingItemResponse;
import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.entity.RankingRegionType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRankingCacheTest {

    private static final String KEY = "ranking:places:2026-04:dong:30";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisRankingCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cache = new RedisRankingCache(
                redisTemplate,
                objectMapper,
                new RankingRedisProperties(Duration.ofMinutes(5))
        );
    }

    @Test
    void getPlaceRanking_returnsCachedRanking() throws Exception {
        PlaceRankingResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(response));

        var result = cache.getPlaceRanking("2026-04", RankingRegionType.DONG, 30L);

        assertThat(result).contains(response);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void putPlaceRanking_storesSerializedRanking() throws Exception {
        PlaceRankingResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.putPlaceRanking("2026-04", RankingRegionType.DONG, 30L, response);

        verify(valueOperations).set(
                eq(KEY),
                eq(objectMapper.writeValueAsString(response)),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void evictAllPlaceRankings_deletesMatchingKeys() {
        when(redisTemplate.keys("ranking:places:*")).thenReturn(Set.of(KEY));

        cache.evictAllPlaceRankings();

        verify(redisTemplate).delete(Set.of(KEY));
    }

    private PlaceRankingResponse response() {
        return new PlaceRankingResponse(
                "2026-04",
                "dong",
                "Seogyo-dong",
                List.of(new PlaceRankingItemResponse(1, 100L, "Seogyo Soup", 1, BigDecimal.TEN, List.of()))
        );
    }
}
