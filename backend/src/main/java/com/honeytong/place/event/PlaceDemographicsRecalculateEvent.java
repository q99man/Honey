package com.honeytong.place.event;

public class PlaceDemographicsRecalculateEvent {
    private final Long placeId;

    public PlaceDemographicsRecalculateEvent(Long placeId) {
        this.placeId = placeId;
    }

    public Long getPlaceId() {
        return placeId;
    }
}
