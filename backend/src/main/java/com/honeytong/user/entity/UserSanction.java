package com.honeytong.user.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sanctions")
public class UserSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 30)
    private UserSanctionType sanctionType;

    @Column(length = 255)
    private String reason;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserSanctionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    protected UserSanction() {
    }

    public UserSanction(
            User user,
            UserSanctionType sanctionType,
            String reason,
            LocalDateTime startAt,
            LocalDateTime endAt,
            User createdBy
    ) {
        this.user = user;
        this.sanctionType = sanctionType;
        this.reason = reason;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = UserSanctionStatus.ACTIVE;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UserSanctionType getSanctionType() {
        return sanctionType;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public UserSanctionStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }
}
