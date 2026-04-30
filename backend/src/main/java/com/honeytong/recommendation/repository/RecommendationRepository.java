package com.honeytong.recommendation.repository;

import com.honeytong.recommendation.entity.Recommendation;
import com.honeytong.recommendation.entity.RecommendationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByUserIdAndPlaceId(Long userId, Long placeId);

    boolean existsByUserIdAndPlaceIdAndStatus(Long userId, Long placeId, RecommendationStatus status);

    long countByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            RecommendationStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Recommendation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RecommendationStatus status);
}
