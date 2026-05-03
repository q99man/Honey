package com.honeytong.auth.verification;

import com.honeytong.auth.entity.PhoneVerificationCode;
import java.time.LocalDateTime;

public record PhoneVerificationState(
        Long verificationCodeId,
        String codeHash,
        LocalDateTime expiresAt,
        int attemptCount
) {

    public static PhoneVerificationState from(PhoneVerificationCode verificationCode) {
        return new PhoneVerificationState(
                verificationCode.getId(),
                verificationCode.getCodeHash(),
                verificationCode.getExpiresAt(),
                verificationCode.getAttemptCount()
        );
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canAttempt(int maxAttempts) {
        return attemptCount < maxAttempts;
    }
}
