package com.honeytong.ranking.repository;

import com.honeytong.ranking.entity.PlaceRankingHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRankingHistoryRepository extends JpaRepository<PlaceRankingHistory, Long> {

    void deleteBySeasonId(Long seasonId);

    @Query("""
            select history from PlaceRankingHistory history
            join fetch history.season
            where history.place.id = :placeId
            order by history.season.startAt desc, history.regionType asc, history.regionRefId asc, history.rankNo asc
            """)
    List<PlaceRankingHistory> findAllByPlaceIdForPublicHistory(@Param("placeId") Long placeId);
}
