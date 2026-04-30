package com.honeytong.recommendation.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.place.entity.Place;
import com.honeytong.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recommendations_user_place",
                columnNames = {"user_id", "place_id"}
        )
)
public class Recommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "recommend_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal recommendWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    protected Recommendation() {
    }

    public Recommendation(User user, Place place, BigDecimal recommendWeight) {
        this.user = user;
        this.place = place;
        this.recommendWeight = recommendWeight;
        this.status = RecommendationStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getRecommendWeight() {
        return recommendWeight;
    }

    public Place getPlace() {
        return place;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == RecommendationStatus.ACTIVE;
    }

    public void activate(BigDecimal recommendWeight) {
        this.recommendWeight = recommendWeight;
        this.status = RecommendationStatus.ACTIVE;
    }

    public void cancel() {
        this.status = RecommendationStatus.CANCELED;
    }
}
