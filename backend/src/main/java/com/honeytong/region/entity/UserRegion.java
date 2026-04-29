package com.honeytong.region.entity;

import com.honeytong.common.entity.BaseTimeEntity;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "user_region")
public class UserRegion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id")
    private RegionCity city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id")
    private RegionDistrict district;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dong_id")
    private RegionDong dong;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryRegion;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRegionStatus status;

    protected UserRegion() {
    }

    public UserRegion(User user, RegionDong dong) {
        this.user = user;
        this.city = dong.getCity();
        this.district = dong.getDistrict();
        this.dong = dong;
        this.primaryRegion = true;
        this.verifiedAt = LocalDateTime.now();
        this.changedAt = LocalDateTime.now();
        this.status = UserRegionStatus.ACTIVE;
    }

    public RegionCity getCity() {
        return city;
    }

    public RegionDistrict getDistrict() {
        return district;
    }

    public RegionDong getDong() {
        return dong;
    }

    public boolean isPrimaryRegion() {
        return primaryRegion;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void deactivate() {
        this.primaryRegion = false;
        this.status = UserRegionStatus.INACTIVE;
        this.changedAt = LocalDateTime.now();
    }
}
