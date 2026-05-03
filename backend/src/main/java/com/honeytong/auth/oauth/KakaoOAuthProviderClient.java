package com.honeytong.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.honeytong.auth.config.OAuthProperties;
import com.honeytong.auth.entity.AuthProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthProviderClient extends AbstractOAuthProviderClient {

    private final OAuthProperties properties;

    public KakaoOAuthProviderClient(OAuthProperties properties, RestClient.Builder restClientBuilder) {
        super(restClientBuilder);
        this.properties = properties;
    }

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.KAKAO;
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String accessToken) {
        JsonNode body = getUserInfo(properties.getKakao().getUserInfoUrl(), accessToken);
        String providerUserId = body.path("id").asText(null);
        if (!StringUtils.hasText(providerUserId)) {
            throw invalidProviderResponse();
        }

        JsonNode account = body.path("kakao_account");
        JsonNode profile = account.path("profile");
        return new OAuthUserProfile(
                providerUserId,
                account.path("email").asText(null),
                profile.path("nickname").asText(null)
        );
    }
}
