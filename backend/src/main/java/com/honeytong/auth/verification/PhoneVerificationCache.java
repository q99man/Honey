package com.honeytong.auth.verification;

import java.util.Optional;
import java.util.function.Supplier;

public interface PhoneVerificationCache {

    Optional<PhoneVerificationState> getLatestUnverified(
            Long userId,
            String phone,
            Supplier<Optional<PhoneVerificationState>> databaseStateSupplier
    );

    void put(Long userId, String phone, PhoneVerificationState state);

    void evict(Long userId, String phone);
}
