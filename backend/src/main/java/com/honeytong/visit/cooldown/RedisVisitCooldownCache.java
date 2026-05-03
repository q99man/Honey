package com.honeytong.visit.cooldown;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisVisitCooldownCache implements VisitCooldownCache {

    private static final String KEY_PREFIX = "visit:cooldown:user:";

    private final StringRedisTemplate redisTemplate;

    public RedisVisitCooldownCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<LocalDateTime> getCooldownUntil(
            Long userId,
            Long placeId,
            Supplier<Optional<LocalDateTime>> databaseCooldownSupplier
    ) {
        String cacheKey = key(userId, placeId);
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null && !cachedValue.isBlank()) {
                LocalDateTime cooldownUntil = LocalDateTime.parse(cachedValue);
                if (cooldownUntil.isAfter(LocalDateTime.now())) {
                    return Optional.of(cooldownUntil);
                }
                evict(userId, placeId);
            }
        } catch (RuntimeException exception) {
            return databaseCooldownSupplier.get();
        }

        Optional<LocalDateTime> databaseCooldown = databaseCooldownSupplier.get();
        databaseCooldown.ifPresent(cooldownUntil -> put(cacheKey, cooldownUntil));
        return databaseCooldown;
    }

    @Override
    public void evict(Long userId, Long placeId) {
        try {
            redisTemplate.delete(key(userId, placeId));
        } catch (RuntimeException exception) {
            // A cache eviction failure must not break the committed visit workflow.
        }
    }

    private void put(String cacheKey, LocalDateTime cooldownUntil) {
        Duration ttl = Duration.between(LocalDateTime.now(), cooldownUntil);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, cooldownUntil.toString(), ttl);
        } catch (RuntimeException exception) {
            // Visit rows remain the durable source of truth.
        }
    }

    private String key(Long userId, Long placeId) {
        return KEY_PREFIX + userId + ":place:" + placeId;
    }
}
