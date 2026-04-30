package com.honeytong.visit.repository;

import com.honeytong.visit.entity.Visit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    Optional<Visit> findTopByUserIdAndPlaceIdAndValidTrueOrderByCreatedAtDesc(Long userId, Long placeId);

    List<Visit> findByUserIdAndValidTrueOrderByCreatedAtDesc(Long userId);

    Optional<Visit> findTopByPlaceIdAndValidTrueOrderByCreatedAtDesc(Long placeId);
}
