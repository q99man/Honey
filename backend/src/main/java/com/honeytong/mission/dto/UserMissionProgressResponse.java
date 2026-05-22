package com.honeytong.mission.dto;

import com.honeytong.mission.entity.Mission;
import com.honeytong.mission.entity.UserMissionProgress;
import java.time.LocalDateTime;

public record UserMissionProgressResponse(
        Long missionId,
        String missionCode,
        String title,
        String description,
        String missionType,
        String targetType,
        int targetCount,
        int rewardExp,
        String rewardBadgeCode,
        int currentCount,
        boolean isCompleted,
        LocalDateTime completedAt,
        boolean rewardClaimed
) {
    public static UserMissionProgressResponse from(UserMissionProgress progress) {
        Mission m = progress.getMission();
        return new UserMissionProgressResponse(
                m.getId(),
                m.getMissionCode(),
                m.getTitle(),
                m.getDescription(),
                m.getMissionType().name(),
                m.getTargetType().name(),
                m.getTargetCount(),
                m.getRewardExp(),
                m.getRewardBadgeCode(),
                progress.getCurrentCount(),
                progress.isCompleted(),
                progress.getCompletedAt(),
                progress.isRewardClaimed()
        );
    }

    public static UserMissionProgressResponse from(Mission mission, int currentCount, boolean isCompleted, LocalDateTime completedAt, boolean rewardClaimed) {
        return new UserMissionProgressResponse(
                mission.getId(),
                mission.getMissionCode(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getMissionType().name(),
                mission.getTargetType().name(),
                mission.getTargetCount(),
                mission.getRewardExp(),
                mission.getRewardBadgeCode(),
                currentCount,
                isCompleted,
                completedAt,
                rewardClaimed
        );
    }
}
