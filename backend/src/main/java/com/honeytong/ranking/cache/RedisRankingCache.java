package com.honeytong.ranking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.entity.RankingRegionType;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisRankingCache implements RankingCache {

    private static final String KEY_PREFIX = "ranking:places:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RankingRedisProperties properties;

    public RedisRankingCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RankingRedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<PlaceRankingResponse> getPlaceRanking(
            String seasonCode,
            RankingRegionType regionType,
            Long regionId
    ) {
        try {
            String rawValue = redisTemplate.opsForValue().get(key(seasonCode, regionType, regionId));
            if (rawValue == null || rawValue.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(rawValue, PlaceRankingResponse.class));
        } catch (RuntimeException | JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void putPlaceRanking(
            String seasonCode,
            RankingRegionType regionType,
            Long regionId,
            PlaceRankingResponse response
    ) {
        try {
            String rawValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key(seasonCode, regionType, regionId), rawValue, properties.rankingCacheTtl());
        } catch (RuntimeException | JsonProcessingException exception) {
            // place_season_scores remain the authoritative read model.
        }
    }

    @Override
    public void evictAllPlaceRankings() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            redisTemplate.delete(keys);
        } catch (RuntimeException exception) {
            // Cache eviction failure must not break committed admin ranking workflows.
        }
    }

    private String key(String seasonCode, RankingRegionType regionType, Long regionId) {
        return KEY_PREFIX + seasonCode + ":" + regionType.toApiValue() + ":" + regionId;
    }
}
