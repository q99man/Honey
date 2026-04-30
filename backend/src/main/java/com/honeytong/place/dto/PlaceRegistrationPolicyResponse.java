package com.honeytong.place.dto;

public record PlaceRegistrationPolicyResponse(
        boolean canRegister,
        String registrationScope,
        int registrationLimit,
        long currentUsage
) {
}
