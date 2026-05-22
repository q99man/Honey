package com.honeytong.mission.repository;

import com.honeytong.mission.entity.Mission;
import com.honeytong.mission.entity.MissionTargetType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("SELECT m FROM Mission m WHERE m.isActive = true " +
           "AND (m.startAt IS NULL OR m.startAt <= :now) " +
           "AND (m.endAt IS NULL OR m.endAt >= :now)")
    List<Mission> findActiveMissions(@Param("now") LocalDateTime now);

    @Query("SELECT m FROM Mission m WHERE m.isActive = true " +
           "AND m.targetType = :targetType " +
           "AND (m.startAt IS NULL OR m.startAt <= :now) " +
           "AND (m.endAt IS NULL OR m.endAt >= :now)")
    List<Mission> findActiveMissionsByTargetType(
            @Param("targetType") MissionTargetType targetType,
            @Param("now") LocalDateTime now
    );
}
