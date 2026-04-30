package com.honeytong.comment.dto;

public record CommentDeleteResponse(
        Long commentId,
        boolean deleted,
        int commentCount
) {
}
