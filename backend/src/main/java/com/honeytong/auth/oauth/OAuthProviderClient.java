package com.honeytong.auth.oauth;

import com.honeytong.auth.entity.AuthProvider;

public interface OAuthProviderClient {

    boolean supports(AuthProvider provider);

    OAuthUserProfile fetchUserProfile(String accessToken);
}
