package com.honeytong.ranking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.ranking.dto.CurrentSeasonResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisSeasonCache implements SeasonCache {

    private static final String KEY = "ranking:seasons:current";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SeasonRedisProperties properties;

    public RedisSeasonCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SeasonRedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<CurrentSeasonResponse> getCurrentSeason() {
        try {
            String rawValue = redisTemplate.opsForValue().get(KEY);
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(rawValue, CurrentSeasonResponse.class));
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void putCurrentSeason(CurrentSeasonResponse response) {
        try {
            String rawValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY, rawValue, properties.seasonCacheTtl());
        } catch (RuntimeException | JsonProcessingException exception) {
            // Database remains the source of truth when Redis is unavailable.
        }
    }

    @Override
    public void evictCurrentSeason() {
        try {
            redisTemplate.delete(KEY);
        } catch (RuntimeException exception) {
            // Cache eviction failure must not break committed admin season workflows.
        }
    }
}
