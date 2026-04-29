package com.honeytong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.honeytong.user.config.GrowthProperties;
import com.honeytong.user.dto.UserActivitySummaryResponse;
import com.honeytong.user.dto.UserProfileUpdateRequest;
import com.honeytong.user.dto.UserStatusResponse;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private UserActivitySummaryReader activitySummaryReader;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                userTrustRepository,
                userLevelRepository,
                activitySummaryReader,
                new GrowthProperties(100)
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    void getMyProfile_returnsCurrentUserProfile() {
        var response = userService.getMyProfile(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.nickname()).isEqualTo("테스터");
        assertThat(response.languagePreference()).isEqualTo("ko");
        assertThat(response.phoneVerified()).isFalse();
    }

    @Test
    void updateMyProfile_updatesNicknameAndLanguagePreference() {
        var response = userService.updateMyProfile(
                USER_ID,
                new UserProfileUpdateRequest("새닉네임", "en")
        );

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.languagePreference()).isEqualTo("en");
    }

    @Test
    void getMyStatus_combinesUserLevelAndTrust() {
        UserTrust trust = new UserTrust(user);
        UserLevel level = new UserLevel(user);
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.of(level));

        UserStatusResponse response = userService.getMyStatus(USER_ID);

        assertThat(response.level()).isEqualTo(1);
        assertThat(response.exp()).isZero();
        assertThat(response.nextLevelExp()).isEqualTo(100);
        assertThat(response.trustGrade()).isEqualTo("SEED_BEE");
        assertThat(response.phoneVerified()).isFalse();
        assertThat(response.regionVerified()).isFalse();
    }

    @Test
    void getMyActivitySummary_usesReader() {
        when(activitySummaryReader.read(USER_ID))
                .thenReturn(new UserActivitySummaryResponse(1, 2, 3, 4));

        UserActivitySummaryResponse response = userService.getMyActivitySummary(USER_ID);

        assertThat(response.recommendedCount()).isEqualTo(1);
        assertThat(response.visitCount()).isEqualTo(2);
        assertThat(response.commentCount()).isEqualTo(3);
        assertThat(response.registeredPlaceCount()).isEqualTo(4);
    }
}
