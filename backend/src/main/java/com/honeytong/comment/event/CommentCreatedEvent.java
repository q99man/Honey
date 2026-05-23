package com.honeytong.comment.event;

public record CommentCreatedEvent(
        Long commentId,
        Long placeId,
        Long commenterId,
        Long placeCreatorId,
        String placeName,
        String commentContent
) {}
