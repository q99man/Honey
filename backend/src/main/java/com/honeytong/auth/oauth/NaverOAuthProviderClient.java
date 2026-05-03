package com.honeytong.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.honeytong.auth.config.OAuthProperties;
import com.honeytong.auth.entity.AuthProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NaverOAuthProviderClient extends AbstractOAuthProviderClient {

    private final OAuthProperties properties;

    public NaverOAuthProviderClient(OAuthProperties properties, RestClient.Builder restClientBuilder) {
        super(restClientBuilder);
        this.properties = properties;
    }

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.NAVER;
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String accessToken) {
        JsonNode body = getUserInfo(properties.getNaver().getUserInfoUrl(), accessToken);
        JsonNode response = body.path("response");
        String providerUserId = response.path("id").asText(null);
        if (!StringUtils.hasText(providerUserId)) {
            throw invalidProviderResponse();
        }

        return new OAuthUserProfile(
                providerUserId,
                response.path("email").asText(null),
                response.path("nickname").asText(null)
        );
    }
}
