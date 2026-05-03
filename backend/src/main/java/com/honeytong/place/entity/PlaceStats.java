package com.honeytong.place.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_stats")
public class PlaceStats extends BaseTimeEntity {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "recommend_count", nullable = false)
    private int recommendCount;

    @Column(name = "visit_count", nullable = false)
    private int visitCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "unique_user_count", nullable = false)
    private int uniqueUserCount;

    @Column(name = "score_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal scoreTotal;

    @Column(name = "manual_adjustment_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal manualAdjustmentScore;

    @Column(name = "recent_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal recentScore;

    @Column(name = "diversity_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal diversityScore;

    @Column(name = "trust_weighted_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal trustWeightedScore;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    protected PlaceStats() {
    }

    public PlaceStats(Place place) {
        this.place = place;
        this.recommendCount = 0;
        this.visitCount = 0;
        this.commentCount = 0;
        this.uniqueUserCount = 0;
        this.scoreTotal = BigDecimal.ZERO;
        this.manualAdjustmentScore = BigDecimal.ZERO;
        this.recentScore = BigDecimal.ZERO;
        this.diversityScore = BigDecimal.ZERO;
        this.trustWeightedScore = BigDecimal.ZERO;
    }

    public int getRecommendCount() {
        return recommendCount;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getUniqueUserCount() {
        return uniqueUserCount;
    }

    public BigDecimal getScoreTotal() {
        return scoreTotal;
    }

    public BigDecimal getManualAdjustmentScore() {
        return manualAdjustmentScore == null ? BigDecimal.ZERO : manualAdjustmentScore;
    }

    public Place getPlace() {
        return place;
    }

    public BigDecimal getRecentScore() {
        return recentScore;
    }

    public BigDecimal getDiversityScore() {
        return diversityScore;
    }

    public BigDecimal getTrustWeightedScore() {
        return trustWeightedScore;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void addRecommendation(BigDecimal recommendWeight) {
        this.recommendCount++;
        this.scoreTotal = this.scoreTotal.add(recommendWeight);
        this.trustWeightedScore = this.trustWeightedScore.add(recommendWeight);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void removeRecommendation(BigDecimal recommendWeight) {
        if (this.recommendCount > 0) {
            this.recommendCount--;
        }
        this.scoreTotal = this.scoreTotal.subtract(recommendWeight).max(BigDecimal.ZERO);
        this.trustWeightedScore = this.trustWeightedScore.subtract(recommendWeight).max(BigDecimal.ZERO);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void addVisit(BigDecimal visitWeight) {
        this.visitCount++;
        this.scoreTotal = this.scoreTotal.add(visitWeight);
        this.trustWeightedScore = this.trustWeightedScore.add(visitWeight);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void removeVisit(BigDecimal visitWeight) {
        if (this.visitCount > 0) {
            this.visitCount--;
        }
        this.scoreTotal = this.scoreTotal.subtract(visitWeight).max(BigDecimal.ZERO);
        this.trustWeightedScore = this.trustWeightedScore.subtract(visitWeight).max(BigDecimal.ZERO);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void addComment(BigDecimal commentWeight) {
        this.commentCount++;
        this.scoreTotal = this.scoreTotal.add(commentWeight);
        this.trustWeightedScore = this.trustWeightedScore.add(commentWeight);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void removeComment(BigDecimal commentWeight) {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
        this.scoreTotal = this.scoreTotal.subtract(commentWeight).max(BigDecimal.ZERO);
        this.trustWeightedScore = this.trustWeightedScore.subtract(commentWeight).max(BigDecimal.ZERO);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void adjustManualScore(BigDecimal scoreDelta) {
        this.manualAdjustmentScore = getManualAdjustmentScore().add(scoreDelta);
    }
}
