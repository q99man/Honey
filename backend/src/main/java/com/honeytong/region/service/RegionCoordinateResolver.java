package com.honeytong.region.service;

import java.math.BigDecimal;

public interface RegionCoordinateResolver {

    ResolvedRegion resolve(BigDecimal latitude, BigDecimal longitude);
}
