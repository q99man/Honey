package com.honeytong.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.growth")
public record GrowthProperties(
        int baseNextLevelExp
) {
}
