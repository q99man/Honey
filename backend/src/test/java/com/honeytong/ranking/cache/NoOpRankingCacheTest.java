package com.honeytong.ranking.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.honeytong.ranking.entity.RankingRegionType;
import org.junit.jupiter.api.Test;

class NoOpRankingCacheTest {

    @Test
    void getPlaceRanking_returnsEmpty() {
        NoOpRankingCache cache = new NoOpRankingCache();

        var result = cache.getPlaceRanking("2026-04", RankingRegionType.DONG, 30L);

        assertThat(result).isEmpty();
    }
}
