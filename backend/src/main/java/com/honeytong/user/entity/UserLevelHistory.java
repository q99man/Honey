package com.honeytong.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_level_history")
public class UserLevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "previous_level", nullable = false)
    private int previousLevel;

    @Column(name = "new_level", nullable = false)
    private int newLevel;

    @Column(name = "changed_reason", length = 100)
    private String changedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserLevelHistory() {
    }

    public UserLevelHistory(User user, int previousLevel, int newLevel, String changedReason) {
        this.user = user;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.changedReason = changedReason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public String getChangedReason() {
        return changedReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
