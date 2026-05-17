package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceStats;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceStatsRepository extends JpaRepository<PlaceStats, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT stats FROM PlaceStats stats WHERE stats.placeId = :placeId")
    Optional<PlaceStats> findByIdForUpdate(@Param("placeId") Long placeId);
}
