package com.honeytong.mission.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
public class Mission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_code", nullable = false, unique = true, length = 50)
    private String missionCode;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 30)
    private MissionType missionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private MissionTargetType targetType;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "reward_exp", nullable = false)
    private int rewardExp;

    @Column(name = "reward_badge_code", length = 50)
    private String rewardBadgeCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    protected Mission() {
    }

    public Mission(
            String missionCode,
            String title,
            String description,
            MissionType missionType,
            MissionTargetType targetType,
            int targetCount,
            int rewardExp,
            String rewardBadgeCode,
            boolean isActive,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.missionCode = missionCode;
        this.title = title;
        this.description = description;
        this.missionType = missionType;
        this.targetType = targetType;
        this.targetCount = targetCount;
        this.rewardExp = rewardExp;
        this.rewardBadgeCode = rewardBadgeCode;
        this.isActive = isActive;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long getId() {
        return id;
    }

    public String getMissionCode() {
        return missionCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public MissionTargetType getTargetType() {
        return targetType;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getRewardExp() {
        return rewardExp;
    }

    public String getRewardBadgeCode() {
        return rewardBadgeCode;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isAvailableAt(LocalDateTime time) {
        if (!isActive) {
            return false;
        }
        if (startAt != null && time.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && time.isAfter(endAt)) {
            return false;
        }
        return true;
    }
}
