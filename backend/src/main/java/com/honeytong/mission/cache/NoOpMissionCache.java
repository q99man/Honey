package com.honeytong.mission.cache;

import com.honeytong.mission.dto.MissionResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMissionCache implements MissionCache {

    @Override
    public Optional<List<MissionResponse>> getActiveMissions() {
        return Optional.empty();
    }

    @Override
    public void putActiveMissions(List<MissionResponse> missions) {
    }

    @Override
    public void evictActiveMissions() {
    }
}
