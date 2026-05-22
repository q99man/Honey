package com.honeytong.mission.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.mission.dto.MissionClaimResponse;
import com.honeytong.mission.dto.MissionResponse;
import com.honeytong.mission.dto.UserMissionProgressResponse;
import com.honeytong.mission.service.MissionService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/missions")
    public ApiResponse<List<MissionResponse>> getActiveMissions() {
        return ApiResponse.success(missionService.getActiveMissions(), "OK");
    }

    @GetMapping("/users/me/missions")
    public ApiResponse<List<UserMissionProgressResponse>> getUserMissionProgresses(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(missionService.getUserMissionProgresses(userId), "OK");
    }

    @PostMapping("/users/me/missions/{missionId}/claim")
    public ApiResponse<MissionClaimResponse> claimReward(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long missionId
    ) {
        return ApiResponse.success(missionService.claimReward(userId, missionId), "Reward claimed");
    }
}
