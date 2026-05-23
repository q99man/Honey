package com.honeytong.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_ai_tags")
public class PlaceAiTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PlaceAiTag() {
    }

    public PlaceAiTag(Place place, String tagName) {
        this.place = place;
        this.tagName = tagName;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Place getPlace() {
        return place;
    }

    public String getTagName() {
        return tagName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
