package com.honeytong.admin.dto;

import com.honeytong.comment.entity.CommentStatus;
import java.time.LocalDateTime;

public record AdminCommentResponse(
        Long commentId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        String categoryCode,
        String content,
        CommentStatus status,
        int reportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
