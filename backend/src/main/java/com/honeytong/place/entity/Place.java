package com.honeytong.place.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_city_id")
    private RegionCity regionCity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_district_id")
    private RegionDistrict regionDistrict;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_dong_id")
    private RegionDong regionDong;

    @Column(name = "address_road", length = 255)
    private String addressRoad;

    @Column(name = "address_jibun", length = 255)
    private String addressJibun;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "price_range_code", length = 30)
    private String priceRangeCode;

    @Column(name = "recommended_menu", length = 255)
    private String recommendedMenu;

    @Column(name = "short_recommendation", nullable = false, length = 255)
    private String shortRecommendation;

    @Column(name = "feature_text", length = 500)
    private String featureText;

    @Column(name = "is_franchise", nullable = false)
    private boolean franchise;

    @Enumerated(EnumType.STRING)
    @Column(name = "franchise_review_status", length = 20)
    private FranchiseReviewStatus franchiseReviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private PlaceApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "exposure_status", nullable = false, length = 20)
    private PlaceExposureStatus exposureStatus;

    @Column(name = "current_star_level", nullable = false)
    private int currentStarLevel;

    @Column(name = "current_flower_grade", length = 30)
    private String currentFlowerGrade;

    @Column(name = "ranking_excluded", nullable = false)
    private boolean rankingExcluded;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Place() {
    }

    public Place(
            User createdBy,
            RegionDong regionDong,
            String name,
            String categoryCode,
            String addressRoad,
            String addressJibun,
            BigDecimal latitude,
            BigDecimal longitude,
            String priceRangeCode,
            String recommendedMenu,
            String shortRecommendation,
            String featureText,
            boolean franchise
    ) {
        this.createdBy = createdBy;
        this.regionCity = regionDong.getCity();
        this.regionDistrict = regionDong.getDistrict();
        this.regionDong = regionDong;
        this.name = name;
        this.categoryCode = categoryCode;
        this.addressRoad = addressRoad;
        this.addressJibun = addressJibun;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRangeCode = priceRangeCode;
        this.recommendedMenu = recommendedMenu;
        this.shortRecommendation = shortRecommendation;
        this.featureText = featureText;
        this.franchise = franchise;
        this.franchiseReviewStatus = FranchiseReviewStatus.PENDING;
        this.approvalStatus = PlaceApprovalStatus.APPROVED;
        this.exposureStatus = PlaceExposureStatus.VISIBLE;
        this.currentStarLevel = 0;
        this.currentFlowerGrade = "SEED";
        this.rankingExcluded = false;
    }

    public Long getId() {
        return id;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public RegionCity getRegionCity() {
        return regionCity;
    }

    public RegionDistrict getRegionDistrict() {
        return regionDistrict;
    }

    public RegionDong getRegionDong() {
        return regionDong;
    }

    public String getAddressRoad() {
        return addressRoad;
    }

    public String getAddressJibun() {
        return addressJibun;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getPriceRangeCode() {
        return priceRangeCode;
    }

    public String getRecommendedMenu() {
        return recommendedMenu;
    }

    public String getShortRecommendation() {
        return shortRecommendation;
    }

    public String getFeatureText() {
        return featureText;
    }

    public boolean isFranchise() {
        return franchise;
    }

    public FranchiseReviewStatus getFranchiseReviewStatus() {
        return franchiseReviewStatus;
    }

    public PlaceApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public PlaceExposureStatus getExposureStatus() {
        return exposureStatus;
    }

    public void changeExposureStatus(PlaceExposureStatus exposureStatus) {
        this.exposureStatus = exposureStatus;
    }

    public void changeApprovalStatus(PlaceApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void changeFranchiseReviewStatus(FranchiseReviewStatus franchiseReviewStatus) {
        this.franchiseReviewStatus = franchiseReviewStatus;
    }

    public int getCurrentStarLevel() {
        return currentStarLevel;
    }

    public void updateCurrentStarLevel(int currentStarLevel) {
        this.currentStarLevel = currentStarLevel;
    }

    public String getCurrentFlowerGrade() {
        return currentFlowerGrade;
    }

    public boolean isRankingExcluded() {
        return rankingExcluded;
    }

    public void changeRankingExcluded(boolean rankingExcluded) {
        this.rankingExcluded = rankingExcluded;
        if (rankingExcluded) {
            this.currentStarLevel = 0;
        }
    }

    public void updateDetails(
            RegionDong regionDong,
            String name,
            String categoryCode,
            String addressRoad,
            String addressJibun,
            BigDecimal latitude,
            BigDecimal longitude,
            String priceRangeCode,
            String recommendedMenu,
            String shortRecommendation,
            String featureText,
            boolean franchise
    ) {
        this.regionCity = regionDong.getCity();
        this.regionDistrict = regionDong.getDistrict();
        this.regionDong = regionDong;
        this.name = name;
        this.categoryCode = categoryCode;
        this.addressRoad = addressRoad;
        this.addressJibun = addressJibun;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRangeCode = priceRangeCode;
        this.recommendedMenu = recommendedMenu;
        this.shortRecommendation = shortRecommendation;
        this.featureText = featureText;
        this.franchise = franchise;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.currentStarLevel = 0;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
