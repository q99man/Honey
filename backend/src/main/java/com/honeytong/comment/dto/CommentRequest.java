package com.honeytong.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank
        @Size(max = 300)
        String content
) {
}
