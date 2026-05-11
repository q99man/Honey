package com.honeytong.place.repository;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    List<Place> findTop50ByNameContainingIgnoreCaseAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
            String keyword,
            PlaceExposureStatus exposureStatus
    );

    @Query("""
            SELECT p
            FROM Place p
            JOIN p.regionCity city
            JOIN p.regionDistrict district
            JOIN p.regionDong dong
            WHERE p.deletedAt IS NULL
              AND p.exposureStatus = :exposureStatus
              AND (
                LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.recommendedMenu, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.shortRecommendation, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.featureText, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.addressRoad, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.addressJibun, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(city.nameKo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(district.nameKo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(dong.nameKo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(city.nameEn, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(district.nameEn, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(dong.nameEn, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.createdAt DESC
            """)
    List<Place> searchVisiblePlaces(
            @Param("keyword") String keyword,
            @Param("exposureStatus") PlaceExposureStatus exposureStatus,
            Pageable pageable
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
