package com.honeytong.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.phone-verification")
public record PhoneVerificationProperties(
        int codeLength,
        long codeTtlMinutes,
        int maxAttempts
) {
}
