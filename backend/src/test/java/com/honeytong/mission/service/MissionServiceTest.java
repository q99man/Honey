package com.honeytong.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.mission.dto.MissionClaimResponse;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long MISSION_ID = 10L;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserMissionProgressRepository userMissionProgressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGrowthService userGrowthService;

    private MissionService missionService;
    private User user;
    private Mission mission;

    @BeforeEach
    void setUp() {
        missionService = new MissionService(
                missionRepository,
                userMissionProgressRepository,
                userRepository,
                userGrowthService
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        mission = new Mission(
                "VISIT_3",
                "방문 3회",
                "지역 상점을 3회 방문하세요.",
                MissionType.REPEATABLE,
                MissionTargetType.VISIT,
                3,
                100,
                "VISIT_BADGE",
                true,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5)
        );
        ReflectionTestUtils.setField(mission, "id", MISSION_ID);
    }

    @Test
    void getActiveMissions_returnsMissions() {
        when(missionRepository.findActiveMissions(any(LocalDateTime.class)))
                .thenReturn(List.of(mission));

        var result = missionService.getActiveMissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).missionCode()).isEqualTo("VISIT_3");
    }

    @Test
    void trackProgress_incrementsProgressForActiveUser() {
        UserMissionProgress progress = new UserMissionProgress(user, mission);
        ReflectionTestUtils.setField(progress, "currentCount", 1);

        when(missionRepository.findActiveMissionsByTargetType(eq(MissionTargetType.VISIT), any(LocalDateTime.class)))
                .thenReturn(List.of(mission));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMissionProgressRepository.findByUserIdAndMissionId(USER_ID, MISSION_ID))
                .thenReturn(Optional.of(progress));

        missionService.trackProgress(USER_ID, MissionTargetType.VISIT);

        assertThat(progress.getCurrentCount()).isEqualTo(2);
        verify(userMissionProgressRepository).save(progress);
    }

    @Test
    void trackProgress_skipsWhenProgressIsAlreadyCompleted() {
        UserMissionProgress progress = new UserMissionProgress(user, mission);
        ReflectionTestUtils.setField(progress, "currentCount", 3);
        ReflectionTestUtils.setField(progress, "isCompleted", true);

        when(missionRepository.findActiveMissionsByTargetType(eq(MissionTargetType.VISIT), any(LocalDateTime.class)))
                .thenReturn(List.of(mission));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMissionProgressRepository.findByUserIdAndMissionId(USER_ID, MISSION_ID))
                .thenReturn(Optional.of(progress));

        missionService.trackProgress(USER_ID, MissionTargetType.VISIT);

        assertThat(progress.getCurrentCount()).isEqualTo(3);
        verify(userMissionProgressRepository, never()).save(any());
    }

    @Test
    void claimReward_throwsExceptionWhenNotCompleted() {
        UserMissionProgress progress = new UserMissionProgress(user, mission);
        ReflectionTestUtils.setField(progress, "currentCount", 1);
        ReflectionTestUtils.setField(progress, "isCompleted", false);

        when(userMissionProgressRepository.findByUserIdAndMissionId(USER_ID, MISSION_ID))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> missionService.claimReward(USER_ID, MISSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("미션이 아직 완료되지 않았습니다.");
    }

    @Test
    void claimReward_throwsExceptionWhenAlreadyClaimed() {
        UserMissionProgress progress = new UserMissionProgress(user, mission);
        ReflectionTestUtils.setField(progress, "currentCount", 3);
        ReflectionTestUtils.setField(progress, "isCompleted", true);
        ReflectionTestUtils.setField(progress, "rewardClaimed", true);

        when(userMissionProgressRepository.findByUserIdAndMissionId(USER_ID, MISSION_ID))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> missionService.claimReward(USER_ID, MISSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 보상을 수령한 미션입니다.");
    }

    @Test
    void claimReward_rewardsExpAndResetsRepeatableMission() {
        UserMissionProgress progress = new UserMissionProgress(user, mission);
        ReflectionTestUtils.setField(progress, "currentCount", 3);
        ReflectionTestUtils.setField(progress, "isCompleted", true);
        ReflectionTestUtils.setField(progress, "rewardClaimed", false);

        when(userMissionProgressRepository.findByUserIdAndMissionId(USER_ID, MISSION_ID))
                .thenReturn(Optional.of(progress));

        MissionClaimResponse response = missionService.claimReward(USER_ID, MISSION_ID);

        assertThat(response.expGained()).isEqualTo(100);
        assertThat(response.rewardBadgeCode()).isEqualTo("VISIT_BADGE");
        
        // Since it's REPEATABLE, progress must reset:
        assertThat(progress.getCurrentCount()).isZero();
        assertThat(progress.isCompleted()).isFalse();
        assertThat(progress.isRewardClaimed()).isFalse();

        verify(userGrowthService).rewardExp(USER_ID, 100, "MISSION_COMPLETE_VISIT_3");
        verify(userMissionProgressRepository).save(progress);
    }
}
