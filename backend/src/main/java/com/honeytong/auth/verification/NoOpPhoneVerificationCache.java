package com.honeytong.auth.verification;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPhoneVerificationCache implements PhoneVerificationCache {

    @Override
    public Optional<PhoneVerificationState> getLatestUnverified(
            Long userId,
            String phone,
            Supplier<Optional<PhoneVerificationState>> databaseStateSupplier
    ) {
        return databaseStateSupplier.get();
    }

    @Override
    public void put(Long userId, String phone, PhoneVerificationState state) {
    }

    @Override
    public void evict(Long userId, String phone) {
    }
}
