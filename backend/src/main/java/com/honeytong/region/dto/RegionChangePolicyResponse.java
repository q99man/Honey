package com.honeytong.region.dto;

import java.time.LocalDateTime;

public record RegionChangePolicyResponse(
        boolean changeAllowed,
        int cooldownDays,
        LocalDateTime nextAvailableAt
) {
}
