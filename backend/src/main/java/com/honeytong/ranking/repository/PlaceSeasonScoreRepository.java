package com.honeytong.ranking.repository;

import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceSeasonScoreRepository extends JpaRepository<PlaceSeasonScore, Long> {

    void deleteBySeasonId(Long seasonId);

    List<PlaceSeasonScore> findTop50BySeasonIdAndRegionTypeAndRegionRefIdOrderByRankNoAscTotalScoreDesc(
            Long seasonId,
            RankingRegionType regionType,
            Long regionRefId
    );
}
