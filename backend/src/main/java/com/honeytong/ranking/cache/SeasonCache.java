package com.honeytong.ranking.cache;

import com.honeytong.ranking.dto.CurrentSeasonResponse;
import java.util.Optional;

public interface SeasonCache {

    Optional<CurrentSeasonResponse> getCurrentSeason();

    void putCurrentSeason(CurrentSeasonResponse response);

    void evictCurrentSeason();
}
