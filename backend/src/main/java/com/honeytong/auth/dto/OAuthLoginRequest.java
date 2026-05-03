package com.honeytong.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank(message = "provider access token is required.")
        String accessToken
) {
}
