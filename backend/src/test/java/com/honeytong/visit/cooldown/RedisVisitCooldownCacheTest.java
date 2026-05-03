package com.honeytong.visit.cooldown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisVisitCooldownCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisVisitCooldownCache cache;

    @BeforeEach
    void setUp() {
        cache = new RedisVisitCooldownCache(redisTemplate);
    }

    @Test
    void getCooldownUntil_returnsCachedCooldownWithoutDatabaseLookup() {
        LocalDateTime cooldownUntil = LocalDateTime.now().plusHours(3);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("visit:cooldown:user:7:place:100")).thenReturn(cooldownUntil.toString());
        AtomicInteger databaseCalls = new AtomicInteger();

        Optional<LocalDateTime> result = cache.getCooldownUntil(7L, 100L, () -> {
            databaseCalls.incrementAndGet();
            return Optional.empty();
        });

        assertThat(result).contains(cooldownUntil);
        assertThat(databaseCalls).hasValue(0);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void getCooldownUntil_cachesDatabaseCooldownWhenCacheMisses() {
        LocalDateTime cooldownUntil = LocalDateTime.now().plusHours(3);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("visit:cooldown:user:7:place:100")).thenReturn(null);

        Optional<LocalDateTime> result = cache.getCooldownUntil(7L, 100L, () -> Optional.of(cooldownUntil));

        assertThat(result).contains(cooldownUntil);
        verify(valueOperations).set(eq("visit:cooldown:user:7:place:100"), eq(cooldownUntil.toString()), any(Duration.class));
    }

    @Test
    void evict_deletesCooldownKey() {
        cache.evict(7L, 100L);

        verify(redisTemplate).delete("visit:cooldown:user:7:place:100");
    }
}
