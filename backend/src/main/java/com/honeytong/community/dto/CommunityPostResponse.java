package com.honeytong.community.dto;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long postId,
        Long authorUserId,
        String authorNickname,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine
) {
}
