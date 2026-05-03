package com.honeytong.ranking.entity;

import com.honeytong.place.entity.Place;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_ranking_history")
public class PlaceRankingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false, length = 20)
    private RankingRegionType regionType;

    @Column(name = "region_ref_id", nullable = false)
    private Long regionRefId;

    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(name = "star_level", nullable = false)
    private int starLevel;

    @Column(name = "total_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PlaceRankingHistory() {
    }

    public PlaceRankingHistory(
            Place place,
            Season season,
            RankingRegionType regionType,
            Long regionRefId,
            int rankNo,
            int starLevel,
            BigDecimal totalScore
    ) {
        this.place = place;
        this.season = season;
        this.regionType = regionType;
        this.regionRefId = regionRefId;
        this.rankNo = rankNo;
        this.starLevel = starLevel;
        this.totalScore = totalScore == null ? BigDecimal.ZERO : totalScore;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Place getPlace() {
        return place;
    }

    public Season getSeason() {
        return season;
    }

    public RankingRegionType getRegionType() {
        return regionType;
    }

    public Long getRegionRefId() {
        return regionRefId;
    }

    public int getRankNo() {
        return rankNo;
    }

    public int getStarLevel() {
        return starLevel;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
