package com.honeytong.auth.oauth;

import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OAuthProviderClientRegistry {

    private final List<OAuthProviderClient> clients;

    public OAuthProviderClientRegistry(List<OAuthProviderClient> clients) {
        this.clients = clients;
    }

    public OAuthUserProfile fetchUserProfile(AuthProvider provider, String accessToken) {
        return clients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INVALID_REQUEST,
                        "OAuth provider verification is not configured."
                ))
                .fetchUserProfile(accessToken);
    }
}
