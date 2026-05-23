package com.honeytong.visit.repository;

import com.honeytong.visit.entity.Visit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    Optional<Visit> findTopByUserIdAndPlaceIdAndValidTrueOrderByCreatedAtDesc(Long userId, Long placeId);

    long countByUserIdAndValidTrue(Long userId);

    List<Visit> findByUserIdAndValidTrueOrderByCreatedAtDesc(Long userId);

    Optional<Visit> findTopByPlaceIdAndValidTrueOrderByCreatedAtDesc(Long placeId);

    List<Visit> findTop50ByOrderByCreatedAtDesc();

    long countByValidTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByPlaceIdAndValidTrueAndCreatedAtGreaterThanEqual(Long placeId, LocalDateTime since);
}

