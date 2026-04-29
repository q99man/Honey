package com.honeytong.user.dto;

public record UserActivitySummaryResponse(
        long recommendedCount,
        long visitCount,
        long commentCount,
        long registeredPlaceCount
) {
}
