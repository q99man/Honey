package com.honeytong.place.repository;

import com.honeytong.place.entity.PlaceAiTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceAiTagRepository extends JpaRepository<PlaceAiTag, Long> {
    List<PlaceAiTag> findByPlaceId(Long placeId);
    void deleteByPlaceId(Long placeId);
}
