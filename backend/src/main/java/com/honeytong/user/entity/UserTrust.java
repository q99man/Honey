package com.honeytong.user.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_trust")
public class UserTrust extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_grade", nullable = false, length = 30)
    private TrustGrade trustGrade;

    @Column(name = "recommend_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal recommendWeight;

    @Column(name = "sanction_count", nullable = false)
    private int sanctionCount;

    @Column(name = "report_received_count", nullable = false)
    private int reportReceivedCount;

    @Column(name = "report_confirmed_count", nullable = false)
    private int reportConfirmedCount;

    @Column(name = "abnormal_activity_score", nullable = false)
    private int abnormalActivityScore;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "region_verified", nullable = false)
    private boolean regionVerified;

    @Column(name = "last_evaluated_at")
    private LocalDateTime lastEvaluatedAt;

    protected UserTrust() {
    }

    public UserTrust(User user) {
        this.user = user;
        this.trustScore = 0;
        this.trustGrade = TrustGrade.SEED_BEE;
        this.recommendWeight = BigDecimal.valueOf(1.00);
        this.sanctionCount = 0;
        this.reportReceivedCount = 0;
        this.reportConfirmedCount = 0;
        this.abnormalActivityScore = 0;
        this.phoneVerified = false;
        this.regionVerified = false;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public void markRegionVerified() {
        this.regionVerified = true;
    }

    public void increaseSanctionCount() {
        this.sanctionCount += 1;
    }

    public void addTrustScore(int trustScoreDelta) {
        this.trustScore += trustScoreDelta;
        this.lastEvaluatedAt = LocalDateTime.now();
    }

    public void applyTrustEvaluation(TrustGrade trustGrade, BigDecimal recommendWeight) {
        this.trustGrade = trustGrade;
        this.recommendWeight = recommendWeight;
        this.lastEvaluatedAt = LocalDateTime.now();
    }

    public void adjustTrust(int trustScore, TrustGrade trustGrade) {
        this.trustScore = trustScore;
        this.trustGrade = trustGrade;
        this.lastEvaluatedAt = LocalDateTime.now();
    }

    public void adjustRecommendWeight(BigDecimal recommendWeight) {
        this.recommendWeight = recommendWeight;
        this.lastEvaluatedAt = LocalDateTime.now();
    }

    public int getTrustScore() {
        return trustScore;
    }

    public TrustGrade getTrustGrade() {
        return trustGrade;
    }

    public BigDecimal getRecommendWeight() {
        return recommendWeight;
    }

    public int getSanctionCount() {
        return sanctionCount;
    }

    public int getReportReceivedCount() {
        return reportReceivedCount;
    }

    public int getReportConfirmedCount() {
        return reportConfirmedCount;
    }

    public int getAbnormalActivityScore() {
        return abnormalActivityScore;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public boolean isRegionVerified() {
        return regionVerified;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }
}
