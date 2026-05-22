package com.honeytong.mission.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.mission.dto.MissionClaimResponse;
import com.honeytong.mission.dto.MissionResponse;
import com.honeytong.mission.dto.UserMissionProgressResponse;
import com.honeytong.mission.entity.Mission;
import com.honeytong.mission.entity.MissionTargetType;
import com.honeytong.mission.entity.MissionType;
import com.honeytong.mission.entity.UserMissionProgress;
import com.honeytong.mission.repository.MissionRepository;
import com.honeytong.mission.repository.UserMissionProgressRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserGrowthService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserMissionProgressRepository userMissionProgressRepository;
    private final UserRepository userRepository;
    private final UserGrowthService userGrowthService;

    public MissionService(
            MissionRepository missionRepository,
            UserMissionProgressRepository userMissionProgressRepository,
            UserRepository userRepository,
            UserGrowthService userGrowthService
    ) {
        this.missionRepository = missionRepository;
        this.userMissionProgressRepository = userMissionProgressRepository;
        this.userRepository = userRepository;
        this.userGrowthService = userGrowthService;
    }

    public List<MissionResponse> getActiveMissions() {
        return missionRepository.findActiveMissions(LocalDateTime.now())
                .stream()
                .map(MissionResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserMissionProgressResponse> getUserMissionProgresses(Long userId) {
        List<Mission> activeMissions = missionRepository.findActiveMissions(LocalDateTime.now());
        List<UserMissionProgress> progresses = userMissionProgressRepository.findAllByUserIdWithMission(userId);

        Map<Long, UserMissionProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getMission().getId(), p -> p));

        return activeMissions.stream()
                .map(mission -> {
                    UserMissionProgress progress = progressMap.get(mission.getId());
                    if (progress != null) {
                        return UserMissionProgressResponse.from(progress);
                    } else {
                        return UserMissionProgressResponse.from(
                                mission,
                                0,
                                false,
                                null,
                                false
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void trackProgress(Long userId, MissionTargetType targetType) {
        List<Mission> activeMissions = missionRepository.findActiveMissionsByTargetType(targetType, LocalDateTime.now());
        if (activeMissions.isEmpty()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return;
        }

        for (Mission mission : activeMissions) {
            UserMissionProgress progress = userMissionProgressRepository.findByUserIdAndMissionId(userId, mission.getId())
                    .orElseGet(() -> new UserMissionProgress(user, mission));

            if (progress.isCompleted()) {
                continue;
            }

            progress.incrementProgress(1);
            userMissionProgressRepository.save(progress);
        }
    }

    @Transactional
    public MissionClaimResponse claimReward(Long userId, Long missionId) {
        UserMissionProgress progress = userMissionProgressRepository.findByUserIdAndMissionId(userId, missionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "미션 진행 정보를 찾을 수 없습니다."));

        if (!progress.isCompleted()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "미션이 아직 완료되지 않았습니다.");
        }

        if (progress.isRewardClaimed()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 보상을 수령한 미션입니다.");
        }

        progress.claimReward();
        userGrowthService.rewardExp(userId, progress.getMission().getRewardExp(), "MISSION_COMPLETE_" + progress.getMission().getMissionCode());

        if (progress.getMission().getMissionType() == MissionType.REPEATABLE) {
            progress.resetProgress(0);
        }

        userMissionProgressRepository.save(progress);

        return new MissionClaimResponse(
                progress.getMission().getRewardExp(),
                progress.getMission().getRewardBadgeCode()
        );
    }
}
