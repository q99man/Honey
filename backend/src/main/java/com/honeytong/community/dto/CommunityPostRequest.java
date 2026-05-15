package com.honeytong.community.dto;

import jakarta.validation.constraints.NotBlank;

public record CommunityPostRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
