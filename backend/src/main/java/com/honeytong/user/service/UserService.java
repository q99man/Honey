package com.honeytong.user.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.config.GrowthProperties;
import com.honeytong.user.dto.UserActivitySummaryResponse;
import com.honeytong.user.dto.UserGrowthResponse;
import com.honeytong.user.dto.UserProfileResponse;
import com.honeytong.user.dto.UserProfileUpdateRequest;
import com.honeytong.user.dto.UserStatusResponse;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserActivitySummaryReader activitySummaryReader;
    private final GrowthProperties growthProperties;

    public UserService(
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            UserLevelRepository userLevelRepository,
            UserActivitySummaryReader activitySummaryReader,
            GrowthProperties growthProperties
    ) {
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.userLevelRepository = userLevelRepository;
        this.activitySummaryReader = activitySummaryReader;
        this.growthProperties = growthProperties;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = getActiveUser(userId);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getActiveUser(userId);
        user.updateProfile(request.nickname(), request.languagePreference());
        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserStatusResponse getMyStatus(Long userId) {
        User user = getActiveUser(userId);
        UserTrust trust = getTrust(userId);
        UserLevel level = getLevel(userId);

        return new UserStatusResponse(
                level.getLevel(),
                level.getExp(),
                calculateNextLevelExp(level),
                trust.getTrustGrade().name(),
                trust.getRecommendWeight(),
                user.isPhoneVerified(),
                trust.isRegionVerified()
        );
    }

    @Transactional(readOnly = true)
    public UserGrowthResponse getMyGrowth(Long userId) {
        getActiveUser(userId);
        UserLevel level = getLevel(userId);

        return new UserGrowthResponse(
                level.getLevel(),
                level.getExp(),
                level.getTitle(),
                level.getAvatarStage()
        );
    }

    @Transactional(readOnly = true)
    public UserActivitySummaryResponse getMyActivitySummary(Long userId) {
        getActiveUser(userId);
        return activitySummaryReader.read(userId);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.isPhoneVerified(),
                user.getLanguagePreference()
        );
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private UserTrust getTrust(Long userId) {
        return userTrustRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "사용자 신뢰 정보를 찾을 수 없습니다."));
    }

    private UserLevel getLevel(Long userId) {
        return userLevelRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "사용자 레벨 정보를 찾을 수 없습니다."));
    }

    private int calculateNextLevelExp(UserLevel level) {
        return level.getLevel() * growthProperties.baseNextLevelExp();
    }
}
