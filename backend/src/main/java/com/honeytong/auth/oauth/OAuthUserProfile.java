package com.honeytong.auth.oauth;

public record OAuthUserProfile(
        String providerUserId,
        String email,
        String nickname
) {
}
