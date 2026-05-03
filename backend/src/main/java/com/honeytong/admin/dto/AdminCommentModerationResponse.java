package com.honeytong.admin.dto;

import com.honeytong.comment.entity.CommentStatus;

public record AdminCommentModerationResponse(
        Long commentId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        String content,
        CommentStatus status,
        int commentCount
) {
}
