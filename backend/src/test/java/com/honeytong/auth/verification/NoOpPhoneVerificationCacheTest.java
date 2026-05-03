package com.honeytong.auth.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoOpPhoneVerificationCacheTest {

    @Test
    void getLatestUnverified_returnsDatabaseState() {
        NoOpPhoneVerificationCache cache = new NoOpPhoneVerificationCache();
        PhoneVerificationState state = new PhoneVerificationState(
                1L,
                "hash",
                LocalDateTime.of(2026, 5, 3, 12, 0),
                0
        );

        Optional<PhoneVerificationState> result = cache.getLatestUnverified(1L, "01012345678", () -> Optional.of(state));

        assertThat(result).contains(state);
    }
}
