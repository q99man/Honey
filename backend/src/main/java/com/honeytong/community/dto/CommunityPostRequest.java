package com.honeytong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String content
) {
}
