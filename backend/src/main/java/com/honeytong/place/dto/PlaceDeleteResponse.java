package com.honeytong.place.dto;

public record PlaceDeleteResponse(
        Long placeId,
        boolean deleted
) {
}
