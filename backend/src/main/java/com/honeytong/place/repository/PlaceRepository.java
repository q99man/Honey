package com.honeytong.place.repository;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    long countByCreatedByIdAndDeletedAtIsNull(Long userId);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Place> findTop50ByDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(PlaceExposureStatus exposureStatus);

    List<Place> findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Place> findTop50ByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Place> findByIdAndDeletedAtIsNull(Long id);

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
