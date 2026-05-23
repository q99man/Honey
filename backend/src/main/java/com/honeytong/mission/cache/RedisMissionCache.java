package com.honeytong.mission.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.mission.dto.MissionResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisMissionCache implements MissionCache {

    private static final String KEY = "mission:active";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MissionRedisProperties properties;

    public RedisMissionCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MissionRedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<List<MissionResponse>> getActiveMissions() {
        try {
            String rawValue = redisTemplate.opsForValue().get(KEY);
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            List<MissionResponse> missions = objectMapper.readValue(
                    rawValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MissionResponse.class)
            );
            return Optional.of(missions);
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void putActiveMissions(List<MissionResponse> missions) {
        try {
            String rawValue = objectMapper.writeValueAsString(missions);
            redisTemplate.opsForValue().set(KEY, rawValue, properties.missionCacheTtl());
        } catch (RuntimeException | JsonProcessingException exception) {
            // DB remains the source of truth when Redis is unavailable.
        }
    }

    @Override
    public void evictActiveMissions() {
        try {
            redisTemplate.delete(KEY);
        } catch (RuntimeException exception) {
            // Cache eviction failure must not break client workflows.
        }
    }
}
