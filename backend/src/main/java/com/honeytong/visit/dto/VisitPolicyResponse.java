package com.honeytong.visit.dto;

import java.time.LocalDateTime;

public record VisitPolicyResponse(
        boolean canVisitNow,
        int radiusMeter,
        LocalDateTime cooldownUntil
) {
}
