package com.honeytong.place.service;

import java.math.BigDecimal;

public interface PlaceAddressCoordinateResolver {

    ResolvedPlaceCoordinate resolve(String address);

    record ResolvedPlaceCoordinate(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
