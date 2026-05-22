package com.honeytong.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_audience_stats")
public class PlaceAudienceStats {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "age_10_count", nullable = false)
    private int age10Count;

    @Column(name = "age_20_count", nullable = false)
    private int age20Count;

    @Column(name = "age_30_count", nullable = false)
    private int age30Count;

    @Column(name = "age_40_count", nullable = false)
    private int age40Count;

    @Column(name = "age_50_plus_count", nullable = false)
    private int age50PlusCount;

    @Column(name = "male_count", nullable = false)
    private int maleCount;

    @Column(name = "female_count", nullable = false)
    private int femaleCount;

    @Column(name = "other_gender_count", nullable = false)
    private int otherGenderCount;

    @Column(name = "foreigner_count", nullable = false)
    private int foreignerCount;

    @Column(name = "korean_count", nullable = false)
    private int koreanCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    protected PlaceAudienceStats() {
    }

    public PlaceAudienceStats(Place place) {
        this.place = place;
        this.age10Count = 0;
        this.age20Count = 0;
        this.age30Count = 0;
        this.age40Count = 0;
        this.age50PlusCount = 0;
        this.maleCount = 0;
        this.femaleCount = 0;
        this.otherGenderCount = 0;
        this.foreignerCount = 0;
        this.koreanCount = 0;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public Place getPlace() {
        return place;
    }

    public int getAge10Count() {
        return age10Count;
    }

    public void setAge10Count(int age10Count) {
        this.age10Count = age10Count;
    }

    public int getAge20Count() {
        return age20Count;
    }

    public void setAge20Count(int age20Count) {
        this.age20Count = age20Count;
    }

    public int getAge30Count() {
        return age30Count;
    }

    public void setAge30Count(int age30Count) {
        this.age30Count = age30Count;
    }

    public int getAge40Count() {
        return age40Count;
    }

    public void setAge40Count(int age40Count) {
        this.age40Count = age40Count;
    }

    public int getAge50PlusCount() {
        return age50PlusCount;
    }

    public void setAge50PlusCount(int age50PlusCount) {
        this.age50PlusCount = age50PlusCount;
    }

    public int getMaleCount() {
        return maleCount;
    }

    public void setMaleCount(int maleCount) {
        this.maleCount = maleCount;
    }

    public int getFemaleCount() {
        return femaleCount;
    }

    public void setFemaleCount(int femaleCount) {
        this.femaleCount = femaleCount;
    }

    public int getOtherGenderCount() {
        return otherGenderCount;
    }

    public void setOtherGenderCount(int otherGenderCount) {
        this.otherGenderCount = otherGenderCount;
    }

    public int getForeignerCount() {
        return foreignerCount;
    }

    public void setForeignerCount(int foreignerCount) {
        this.foreignerCount = foreignerCount;
    }

    public int getKoreanCount() {
        return koreanCount;
    }

    public void setKoreanCount(int koreanCount) {
        this.koreanCount = koreanCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
