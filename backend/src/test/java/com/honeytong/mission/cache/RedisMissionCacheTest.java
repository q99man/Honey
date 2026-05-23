package com.honeytong.mission.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.honeytong.mission.dto.MissionResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisMissionCacheTest {

    private static final String KEY = "mission:active";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisMissionCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new RedisMissionCache(
                redisTemplate,
                objectMapper,
                new MissionRedisProperties(Duration.ofMinutes(10))
        );
    }

    @Test
    void getActiveMissions_returnsCachedMissions() throws Exception {
        List<MissionResponse> missions = missions();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(missions));

        var result = cache.getActiveMissions();

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0).missionCode()).isEqualTo("VISIT_3");
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void putActiveMissions_storesSerializedMissions() throws Exception {
        List<MissionResponse> missions = missions();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.putActiveMissions(missions);

        verify(valueOperations).set(
                eq(KEY),
                eq(objectMapper.writeValueAsString(missions)),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void evictActiveMissions_deletesKey() {
        cache.evictActiveMissions();
        verify(redisTemplate).delete(KEY);
    }

    private List<MissionResponse> missions() {
        return List.of(new MissionResponse(
                1L,
                "VISIT_3",
                "맛집 탐방가",
                "맛집을 3회 방문해 보세요.",
                "ONCE",
                "VISIT",
                3,
                50,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59)
        ));
    }
}
