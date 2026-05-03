package com.honeytong.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.honeytong.auth.config.OAuthProperties;
import com.honeytong.auth.entity.AuthProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthProviderClient extends AbstractOAuthProviderClient {

    private final OAuthProperties properties;

    public GoogleOAuthProviderClient(OAuthProperties properties, RestClient.Builder restClientBuilder) {
        super(restClientBuilder);
        this.properties = properties;
    }

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String accessToken) {
        JsonNode body = getUserInfo(properties.getGoogle().getUserInfoUrl(), accessToken);
        String providerUserId = body.path("sub").asText(null);
        if (!StringUtils.hasText(providerUserId)) {
            throw invalidProviderResponse();
        }

        return new OAuthUserProfile(
                providerUserId,
                body.path("email").asText(null),
                body.path("name").asText(null)
        );
    }
}
