package com.honeytong.visit.cooldown;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

public interface VisitCooldownCache {

    Optional<LocalDateTime> getCooldownUntil(
            Long userId,
            Long placeId,
            Supplier<Optional<LocalDateTime>> databaseCooldownSupplier
    );

    void evict(Long userId, Long placeId);
}
