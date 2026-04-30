package com.honeytong.place.repository;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    long countByCreatedByIdAndDeletedAtIsNull(Long userId);

    List<Place> findTop50ByDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(PlaceExposureStatus exposureStatus);

    List<Place> findByDeletedAtIsNullAndExposureStatus(PlaceExposureStatus exposureStatus);

    List<Place> findTop50ByNameContainingIgnoreCaseAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
            String keyword,
            PlaceExposureStatus exposureStatus
    );

    List<Place> findTop50ByRegionCityIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
            Long cityId,
            PlaceExposureStatus exposureStatus
    );

    List<Place> findTop50ByRegionDistrictIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
            Long districtId,
            PlaceExposureStatus exposureStatus
    );

    List<Place> findTop50ByRegionDongIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
            Long dongId,
            PlaceExposureStatus exposureStatus
    );
}
