package com.honeytong.ranking.cache;

import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.entity.RankingRegionType;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpRankingCache implements RankingCache {

    @Override
    public Optional<PlaceRankingResponse> getPlaceRanking(String seasonCode, RankingRegionType regionType, Long regionId) {
        return Optional.empty();
    }

    @Override
    public void putPlaceRanking(String seasonCode, RankingRegionType regionType, Long regionId, PlaceRankingResponse response) {
    }

    @Override
    public void evictAllPlaceRankings() {
    }
}
