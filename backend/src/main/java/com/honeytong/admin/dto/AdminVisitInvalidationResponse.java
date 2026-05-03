package com.honeytong.admin.dto;

public record AdminVisitInvalidationResponse(
        Long visitId,
        Long userId,
        String nickname,
        Long placeId,
        String placeName,
        boolean valid,
        String validReason,
        int visitCount
) {
}
