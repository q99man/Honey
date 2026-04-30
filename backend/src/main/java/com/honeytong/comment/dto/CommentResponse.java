package com.honeytong.comment.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long placeId,
        String placeName,
        Long userId,
        String nickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
