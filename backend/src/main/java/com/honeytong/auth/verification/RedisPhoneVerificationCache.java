package com.honeytong.auth.verification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisPhoneVerificationCache implements PhoneVerificationCache {

    private static final String KEY_PREFIX = "phone:verification:user:";
    private static final String VALUE_SEPARATOR = "\n";

    private final StringRedisTemplate redisTemplate;

    public RedisPhoneVerificationCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<PhoneVerificationState> getLatestUnverified(
            Long userId,
            String phone,
            Supplier<Optional<PhoneVerificationState>> databaseStateSupplier
    ) {
        String cacheKey = key(userId, phone);
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null && !cachedValue.isBlank()) {
                Optional<PhoneVerificationState> cachedState = parse(cachedValue);
                if (cachedState.isPresent() && !cachedState.get().isExpired()) {
                    return cachedState;
                }
                evict(userId, phone);
            }
        } catch (RuntimeException exception) {
            return databaseStateSupplier.get();
        }

        Optional<PhoneVerificationState> databaseState = databaseStateSupplier.get();
        databaseState.ifPresent(state -> put(userId, phone, state));
        return databaseState;
    }

    @Override
    public void put(Long userId, String phone, PhoneVerificationState state) {
        if (state.verificationCodeId() == null) {
            return;
        }
        Duration ttl = Duration.between(LocalDateTime.now(), state.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            evict(userId, phone);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(userId, phone), serialize(state), ttl);
        } catch (RuntimeException exception) {
            // Verification rows remain the durable source of truth.
        }
    }

    @Override
    public void evict(Long userId, String phone) {
        try {
            redisTemplate.delete(key(userId, phone));
        } catch (RuntimeException exception) {
            // A cache eviction failure must not break the committed verification workflow.
        }
    }

    private Optional<PhoneVerificationState> parse(String rawValue) {
        String[] parts = rawValue.split(VALUE_SEPARATOR, 4);
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new PhoneVerificationState(
                    Long.parseLong(parts[0]),
                    parts[3],
                    LocalDateTime.parse(parts[1]),
                    Integer.parseInt(parts[2])
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String serialize(PhoneVerificationState state) {
        return state.verificationCodeId()
                + VALUE_SEPARATOR
                + state.expiresAt()
                + VALUE_SEPARATOR
                + state.attemptCount()
                + VALUE_SEPARATOR
                + state.codeHash();
    }

    private String key(Long userId, String phone) {
        return KEY_PREFIX + userId + ":phone:" + phone;
    }
}
