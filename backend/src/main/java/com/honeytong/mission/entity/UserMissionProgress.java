package com.honeytong.mission.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_mission_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_mission_progress", columnNames = {"user_id", "mission_id"})
        }
)
public class UserMissionProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "current_count", nullable = false)
    private int currentCount = 0;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed = false;

    protected UserMissionProgress() {
    }

    public UserMissionProgress(User user, Mission mission) {
        this.user = user;
        this.mission = mission;
        this.currentCount = 0;
        this.isCompleted = false;
        this.rewardClaimed = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Mission getMission() {
        return mission;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void incrementProgress(int amount) {
        if (isCompleted) {
            return;
        }
        this.currentCount += amount;
        if (this.currentCount >= mission.getTargetCount()) {
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void claimReward() {
        this.rewardClaimed = true;
    }

    public void resetProgress(int newCount) {
        this.currentCount = newCount;
        this.isCompleted = newCount >= mission.getTargetCount();
        this.completedAt = this.isCompleted ? LocalDateTime.now() : null;
        this.rewardClaimed = false;
    }
}
