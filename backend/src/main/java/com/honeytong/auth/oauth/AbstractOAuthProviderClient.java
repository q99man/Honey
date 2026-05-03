package com.honeytong.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

abstract class AbstractOAuthProviderClient implements OAuthProviderClient {

    private final RestClient restClient;

    protected AbstractOAuthProviderClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    protected JsonNode getUserInfo(String userInfoUrl, String accessToken) {
        try {
            JsonNode body = restClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                throw invalidProviderResponse();
            }
            return body;
        } catch (RestClientException ex) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "OAuth provider verification failed.");
        }
    }

    protected ApiException invalidProviderResponse() {
        return new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "OAuth provider user info response is invalid.");
    }
}
