package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    List<PlaceImage> findByPlaceIdOrderBySortOrderAsc(Long placeId);

    void deleteByPlaceId(Long placeId);
}
