package com.honeytong.ranking.entity;

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
@Table(name = "seasons")
public class Season extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_code", nullable = false, length = 30, unique = true)
    private String seasonCode;

    @Column(name = "season_name", nullable = false, length = 100)
    private String seasonName;

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", nullable = false, length = 20)
    private SeasonType seasonType;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonStatus status;

    protected Season() {
    }

    public Season(
            String seasonCode,
            String seasonName,
            SeasonType seasonType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            SeasonStatus status
    ) {
        this.seasonCode = seasonCode;
        this.seasonName = seasonName;
        this.seasonType = seasonType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getSeasonCode() {
        return seasonCode;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public SeasonStatus getStatus() {
        return status;
    }

    public void update(
            String seasonName,
            SeasonType seasonType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            SeasonStatus status
    ) {
        this.seasonName = seasonName;
        this.seasonType = seasonType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }
}
