package com.honeytong.ranking.cache;

import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.entity.RankingRegionType;
import java.util.Optional;

public interface RankingCache {

    Optional<PlaceRankingResponse> getPlaceRanking(String seasonCode, RankingRegionType regionType, Long regionId);

    void putPlaceRanking(String seasonCode, RankingRegionType regionType, Long regionId, PlaceRankingResponse response);

    void evictAllPlaceRankings();
}
