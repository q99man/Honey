package com.honeytong.visit.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.place.entity.Place;
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
import java.math.BigDecimal;

@Entity
@Table(name = "visits")
public class Visit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "distance_meter")
    private Integer distanceMeter;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "valid_reason", length = 100)
    private String validReason;

    protected Visit() {
    }

    public Visit(
            User user,
            Place place,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer distanceMeter,
            String imageUrl,
            boolean valid,
            String validReason
    ) {
        this.user = user;
        this.place = place;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeter = distanceMeter;
        this.imageUrl = imageUrl;
        this.valid = valid;
        this.validReason = validReason;
    }

    public Long getId() {
        return id;
    }

    public Place getPlace() {
        return place;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Integer getDistanceMeter() {
        return distanceMeter;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isValid() {
        return valid;
    }
}
