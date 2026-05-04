package com.honeytong.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.phone-verification.sender")
public record PhoneVerificationSenderProperties(
        String provider,
        String messageTemplate,
        NaverSens naverSens,
        Solapi solapi
) {

    public String renderMessage(String code) {
        return safeMessageTemplate().replace("{code}", code);
    }

    public String safeMessageTemplate() {
        if (messageTemplate == null || messageTemplate.isBlank()) {
            return "[Honeytong] 인증번호 {code}를 입력해 주세요.";
        }
        return messageTemplate;
    }

    public NaverSens safeNaverSens() {
        if (naverSens == null) {
            return new NaverSens(
                    "",
                    "",
                    "",
                    "",
                    "https://sens.apigw.ntruss.com",
                    "82",
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(5)
            );
        }
        return naverSens.withDefaults();
    }

    public Solapi safeSolapi() {
        if (solapi == null) {
            return new Solapi(
                    "",
                    "",
                    "",
                    "https://api.solapi.com",
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(5)
            );
        }
        return solapi.withDefaults();
    }

    public record NaverSens(
            String serviceId,
            String accessKey,
            String secretKey,
            String from,
            String baseUrl,
            String countryCode,
            Duration connectTimeout,
            Duration readTimeout
    ) {

        private NaverSens withDefaults() {
            return new NaverSens(
                    serviceId,
                    accessKey,
                    secretKey,
                    from,
                    hasText(baseUrl) ? baseUrl : "https://sens.apigw.ntruss.com",
                    hasText(countryCode) ? countryCode : "82",
                    connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout,
                    readTimeout == null ? Duration.ofSeconds(5) : readTimeout
            );
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    public record Solapi(
            String apiKey,
            String apiSecret,
            String from,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {

        private Solapi withDefaults() {
            return new Solapi(
                    apiKey,
                    apiSecret,
                    from,
                    hasText(baseUrl) ? baseUrl : "https://api.solapi.com",
                    connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout,
                    readTimeout == null ? Duration.ofSeconds(5) : readTimeout
            );
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
