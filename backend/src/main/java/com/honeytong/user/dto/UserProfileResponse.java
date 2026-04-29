package com.honeytong.user.dto;

public record UserProfileResponse(
        Long id,
        String nickname,
        boolean phoneVerified,
        String languagePreference
) {
}
