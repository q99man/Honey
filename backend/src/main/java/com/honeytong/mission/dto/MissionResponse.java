package com.honeytong.mission.dto;

import com.honeytong.mission.entity.Mission;
import java.time.LocalDateTime;

public record MissionResponse(
        Long id,
        String missionCode,
        String title,
        String description,
        String missionType,
        String targetType,
        int targetCount,
        int rewardExp,
        String rewardBadgeCode,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getMissionCode(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getMissionType().name(),
                mission.getTargetType().name(),
                mission.getTargetCount(),
                mission.getRewardExp(),
                mission.getRewardBadgeCode(),
                mission.getStartAt(),
                mission.getEndAt()
        );
    }
}
