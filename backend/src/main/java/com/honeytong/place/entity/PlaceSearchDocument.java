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

@Entity
@Table(name = "place_search_documents")
public class PlaceSearchDocument extends BaseTimeEntity {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "search_text", nullable = false, length = 3000)
    private String searchText;

    protected PlaceSearchDocument() {
    }

    public PlaceSearchDocument(Place place, String searchText) {
        this.placeId = place.getId();
        this.place = place;
        this.searchText = searchText;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public Place getPlace() {
        return place;
    }

    public String getSearchText() {
        return searchText;
    }
}
