package com.honeytong.mission.repository;

import com.honeytong.mission.entity.UserMissionProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMissionProgressRepository extends JpaRepository<UserMissionProgress, Long> {

    Optional<UserMissionProgress> findByUserIdAndMissionId(Long userId, Long missionId);

    @Query("SELECT up FROM UserMissionProgress up " +
           "JOIN FETCH up.mission m " +
           "WHERE up.user.id = :userId")
    List<UserMissionProgress> findAllByUserIdWithMission(@Param("userId") Long userId);
}
