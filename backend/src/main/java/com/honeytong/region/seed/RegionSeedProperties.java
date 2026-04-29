package com.honeytong.region.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.regions.seed")
public record RegionSeedProperties(
        boolean enabled,
        String location
) {

    public RegionSeedProperties {
        if (location == null || location.isBlank()) {
            location = "classpath:region/region-seed.csv";
        }
    }
}
