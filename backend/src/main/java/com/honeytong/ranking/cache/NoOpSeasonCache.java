package com.honeytong.ranking.cache;

import com.honeytong.ranking.dto.CurrentSeasonResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSeasonCache implements SeasonCache {

    @Override
    public Optional<CurrentSeasonResponse> getCurrentSeason() {
        return Optional.empty();
    }

    @Override
    public void putCurrentSeason(CurrentSeasonResponse response) {
    }

    @Override
    public void evictCurrentSeason() {
    }
}
