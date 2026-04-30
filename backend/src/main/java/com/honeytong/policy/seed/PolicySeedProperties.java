package com.honeytong.policy.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.policies.seed")
public record PolicySeedProperties(
        boolean enabled,
        String location
) {

    public PolicySeedProperties {
        if (location == null || location.isBlank()) {
            location = "classpath:policy/policy-defaults.csv";
        }
    }
}
