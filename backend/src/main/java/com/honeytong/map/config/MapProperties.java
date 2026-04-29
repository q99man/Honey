package com.honeytong.map.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.maps")
public record MapProperties(
        String provider,
        Kakao kakao
) {

    public record Kakao(
            String restApiKey,
            String javascriptKey,
            String localBaseUrl
    ) {
    }
}
