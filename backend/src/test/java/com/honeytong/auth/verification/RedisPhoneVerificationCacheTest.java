package com.honeytong.auth.verification;

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
class RedisPhoneVerificationCacheTest {

    private static final String KEY = "phone:verification:user:7:phone:01012345678";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisPhoneVerificationCache cache;

    @BeforeEach
    void setUp() {
        cache = new RedisPhoneVerificationCache(redisTemplate);
    }

    @Test
    void getLatestUnverified_returnsCachedStateWithoutDatabaseLookup() {
        PhoneVerificationState state = activeState();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(serialize(state));
        AtomicInteger databaseCalls = new AtomicInteger();

        Optional<PhoneVerificationState> result = cache.getLatestUnverified(7L, "01012345678", () -> {
            databaseCalls.incrementAndGet();
            return Optional.empty();
        });

        assertThat(result).contains(state);
        assertThat(databaseCalls).hasValue(0);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void getLatestUnverified_cachesDatabaseStateWhenCacheMisses() {
        PhoneVerificationState state = activeState();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        Optional<PhoneVerificationState> result = cache.getLatestUnverified(7L, "01012345678", () -> Optional.of(state));

        assertThat(result).contains(state);
        verify(valueOperations).set(eq(KEY), eq(serialize(state)), any(Duration.class));
    }

    @Test
    void evict_deletesVerificationKey() {
        cache.evict(7L, "01012345678");

        verify(redisTemplate).delete(KEY);
    }

    private PhoneVerificationState activeState() {
        return new PhoneVerificationState(501L, "hash-value", LocalDateTime.now().plusMinutes(5), 1);
    }

    private String serialize(PhoneVerificationState state) {
        return state.verificationCodeId()
                + "\n"
                + state.expiresAt()
                + "\n"
                + state.attemptCount()
                + "\n"
                + state.codeHash();
    }
}
