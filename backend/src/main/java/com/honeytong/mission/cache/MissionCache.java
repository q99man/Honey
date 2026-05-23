package com.honeytong.mission.cache;

import com.honeytong.mission.dto.MissionResponse;
import java.util.List;
import java.util.Optional;

public interface MissionCache {

    Optional<List<MissionResponse>> getActiveMissions();

    void putActiveMissions(List<MissionResponse> missions);

    void evictActiveMissions();
}
