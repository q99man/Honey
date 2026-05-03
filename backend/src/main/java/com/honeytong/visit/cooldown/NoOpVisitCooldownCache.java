package com.honeytong.visit.cooldown;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVisitCooldownCache implements VisitCooldownCache {

    @Override
    public Optional<LocalDateTime> getCooldownUntil(
            Long userId,
            Long placeId,
            Supplier<Optional<LocalDateTime>> databaseCooldownSupplier
    ) {
        return databaseCooldownSupplier.get();
    }

    @Override
    public void evict(Long userId, Long placeId) {
    }
}
