package com.honeytong.place.entity;

import com.honeytong.common.entity.BaseTimeEntity;
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

@Entity
@Table(name = "place_images")
public class PlaceImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_representative", nullable = false)
    private boolean representative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    protected PlaceImage() {
    }

    public PlaceImage(Place place, String imageUrl, int sortOrder, boolean representative, User uploadedBy) {
        this.place = place;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.representative = representative;
        this.uploadedBy = uploadedBy;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isRepresentative() {
        return representative;
    }
}
