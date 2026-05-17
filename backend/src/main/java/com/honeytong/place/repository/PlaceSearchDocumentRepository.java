package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceSearchDocument;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceSearchDocumentRepository extends JpaRepository<PlaceSearchDocument, Long> {

    @Query("""
            SELECT document
            FROM PlaceSearchDocument document
            JOIN document.place place
            WHERE place.deletedAt IS NULL
              AND place.exposureStatus = :exposureStatus
              AND document.searchText LIKE CONCAT('%', :keyword, '%')
            ORDER BY place.createdAt DESC
            """)
    List<PlaceSearchDocument> searchVisible(
            @Param("keyword") String keyword,
            @Param("exposureStatus") PlaceExposureStatus exposureStatus,
            Pageable pageable
    );
}
