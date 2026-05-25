package com.honeytong.place.repository;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
            SELECT p.*
            FROM places p
            WHERE p.deleted_at IS NULL
              AND p.exposure_status = :exposureStatus
              AND ST_Distance_Sphere(p.location, ST_PointFromText(:pointText, 4326, 'axis-order=long-lat')) <= :radiusMeter
            ORDER BY ST_Distance_Sphere(p.location, ST_PointFromText(:pointText, 4326, 'axis-order=long-lat')) ASC
            LIMIT 50
            """, nativeQuery = true)
    List<Place> findNearbyPlaces(
            @Param("pointText") String pointText,
            @Param("exposureStatus") String exposureStatus,
            @Param("radiusMeter") int radiusMeter
    );
}
