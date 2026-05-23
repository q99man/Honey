package com.honeytong.mission.event;

public record MissionCompletedEvent(
        Long userId,
        Long missionId,
        String missionTitle,
        int rewardExp
) {}
