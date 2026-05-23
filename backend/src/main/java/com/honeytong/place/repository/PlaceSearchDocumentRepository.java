package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceSearchDocument;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceSearchDocumentRepository extends JpaRepository<PlaceSearchDocument, Long> {

    @Query(value = """
            SELECT d.*
            FROM place_search_documents d
            JOIN places p ON d.place_id = p.id
            WHERE p.deleted_at IS NULL
              AND p.exposure_status = :exposureStatus
              AND MATCH(d.search_text) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<PlaceSearchDocument> searchVisibleFts(
            @Param("keyword") String keyword,
            @Param("exposureStatus") String exposureStatus,
            Pageable pageable
    );

    @Query(value = """
            SELECT d.*
            FROM place_search_documents d
            JOIN places p ON d.place_id = p.id
            WHERE p.deleted_at IS NULL
              AND p.exposure_status = :exposureStatus
              AND d.search_text LIKE CONCAT('%', :keyword, '%')
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<PlaceSearchDocument> searchVisibleLike(
            @Param("keyword") String keyword,
            @Param("exposureStatus") String exposureStatus,
            Pageable pageable
    );
}
