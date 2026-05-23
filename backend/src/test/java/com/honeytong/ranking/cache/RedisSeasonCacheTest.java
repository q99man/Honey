package com.honeytong.ranking.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.honeytong.ranking.dto.CurrentSeasonResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisSeasonCacheTest {

    private static final String KEY = "ranking:seasons:current";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisSeasonCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new RedisSeasonCache(
                redisTemplate,
                objectMapper,
                new SeasonRedisProperties(Duration.ofHours(1))
        );
    }

    @Test
    void getCurrentSeason_returnsCachedSeason() throws Exception {
        CurrentSeasonResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(response));

        var result = cache.getCurrentSeason();

        assertThat(result).isPresent();
        assertThat(result.get().seasonCode()).isEqualTo("2026-05");
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void putCurrentSeason_storesSerializedSeason() throws Exception {
        CurrentSeasonResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.putCurrentSeason(response);

        verify(valueOperations).set(
                eq(KEY),
                eq(objectMapper.writeValueAsString(response)),
                eq(Duration.ofHours(1))
        );
    }

    @Test
    void evictCurrentSeason_deletesKey() {
        cache.evictCurrentSeason();
        verify(redisTemplate).delete(KEY);
    }

    private CurrentSeasonResponse response() {
        return new CurrentSeasonResponse(
                "2026-05",
                "May Season",
                "MONTHLY",
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59),
                "ACTIVE"
        );
    }
}
