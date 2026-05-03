package com.honeytong.visit.cooldown;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoOpVisitCooldownCacheTest {

    @Test
    void getCooldownUntil_returnsDatabaseCooldown() {
        NoOpVisitCooldownCache cache = new NoOpVisitCooldownCache();
        LocalDateTime cooldownUntil = LocalDateTime.of(2026, 5, 2, 15, 0);

        Optional<LocalDateTime> result = cache.getCooldownUntil(1L, 100L, () -> Optional.of(cooldownUntil));

        assertThat(result).contains(cooldownUntil);
    }
}
