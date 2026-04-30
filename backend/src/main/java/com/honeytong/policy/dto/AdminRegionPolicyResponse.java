package com.honeytong.policy.dto;

public record AdminRegionPolicyResponse(
        int regionChangeCooldownDays,
        String registrationScope
) {
}
