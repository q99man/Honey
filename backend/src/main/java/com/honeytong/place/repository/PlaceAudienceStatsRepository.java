package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceAudienceStats;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceAudienceStatsRepository extends JpaRepository<PlaceAudienceStats, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT stats FROM PlaceAudienceStats stats WHERE stats.placeId = :placeId")
    Optional<PlaceAudienceStats> findByIdForUpdate(@Param("placeId") Long placeId);
}
