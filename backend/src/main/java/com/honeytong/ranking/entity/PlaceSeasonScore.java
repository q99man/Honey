package com.honeytong.ranking.entity;

import com.honeytong.common.entity.BaseTimeEntity;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "place_season_scores")
public class PlaceSeasonScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false, length = 20)
    private RankingRegionType regionType;

    @Column(name = "region_ref_id", nullable = false)
    private Long regionRefId;

    @Column(name = "recommend_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendScore;

    @Column(name = "visit_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal visitScore;

    @Column(name = "comment_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal commentScore;

    @Column(name = "recent_bonus_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal recentBonusScore;

    @Column(name = "diversity_bonus_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal diversityBonusScore;

    @Column(name = "trust_bonus_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal trustBonusScore;

    @Column(name = "total_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "star_level", nullable = false)
    private int starLevel;

    protected PlaceSeasonScore() {
    }

    public PlaceSeasonScore(
            Season season,
            Place place,
            RankingRegionType regionType,
            Long regionRefId,
            BigDecimal recommendScore,
            BigDecimal visitScore,
            BigDecimal commentScore,
            BigDecimal recentBonusScore,
            BigDecimal diversityBonusScore,
            BigDecimal trustBonusScore,
            BigDecimal totalScore,
            Integer rankNo,
            int starLevel
    ) {
        this.season = season;
        this.place = place;
        this.regionType = regionType;
        this.regionRefId = regionRefId;
        this.recommendScore = valueOrZero(recommendScore);
        this.visitScore = valueOrZero(visitScore);
        this.commentScore = valueOrZero(commentScore);
        this.recentBonusScore = valueOrZero(recentBonusScore);
        this.diversityBonusScore = valueOrZero(diversityBonusScore);
        this.trustBonusScore = valueOrZero(trustBonusScore);
        this.totalScore = valueOrZero(totalScore);
        this.rankNo = rankNo;
        this.starLevel = starLevel;
    }

    public Long getId() {
        return id;
    }

    public Season getSeason() {
        return season;
    }

    public Place getPlace() {
        return place;
    }

    public RankingRegionType getRegionType() {
        return regionType;
    }

    public Long getRegionRefId() {
        return regionRefId;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public Integer getRankNo() {
        return rankNo;
    }

    public int getStarLevel() {
        return starLevel;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
