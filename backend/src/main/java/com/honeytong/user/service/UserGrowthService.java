package com.honeytong.user.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserLevelHistory;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelHistoryRepository;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGrowthService {

    private static final String VALID_VISIT_LEVEL_REASON = "VALID_VISIT";

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserLevelHistoryRepository userLevelHistoryRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserGrowthPolicyService userGrowthPolicyService;

    public UserGrowthService(
            UserRepository userRepository,
            UserLevelRepository userLevelRepository,
            UserLevelHistoryRepository userLevelHistoryRepository,
            UserTrustRepository userTrustRepository,
            UserGrowthPolicyService userGrowthPolicyService
    ) {
        this.userRepository = userRepository;
        this.userLevelRepository = userLevelRepository;
        this.userLevelHistoryRepository = userLevelHistoryRepository;
        this.userTrustRepository = userTrustRepository;
        this.userGrowthPolicyService = userGrowthPolicyService;
    }

    @Transactional
    public VisitGrowthResult applyValidVisit(Long userId) {
        User user = getActiveUser(userId);
        int visitExp = userGrowthPolicyService.getVisitExp();
        int trustScoreDelta = userGrowthPolicyService.getValidVisitScore();

        UserLevel level = userLevelRepository.findById(userId)
                .orElseGet(() -> userLevelRepository.save(new UserLevel(user)));
        UserTrust trust = userTrustRepository.findById(userId)
                .orElseGet(() -> userTrustRepository.save(new UserTrust(user)));

        level.addExp(visitExp);
        applyLevelUp(user, level, VALID_VISIT_LEVEL_REASON);
        trust.addTrustScore(trustScoreDelta);
        TrustEvaluationResult trustEvaluation = userGrowthPolicyService.evaluateTrust(trust.getTrustScore());
        trust.applyTrustEvaluation(trustEvaluation.trustGrade(), trustEvaluation.recommendWeight());

        return new VisitGrowthResult(visitExp, trustScoreDelta);
    }

    @Transactional
    public void rewardExp(Long userId, int exp, String reason) {
        User user = getActiveUser(userId);
        UserLevel level = userLevelRepository.findById(userId)
                .orElseGet(() -> userLevelRepository.save(new UserLevel(user)));

        level.addExp(exp);
        applyLevelUp(user, level, reason);
    }

    private void applyLevelUp(User user, UserLevel level, String reason) {
        int previousLevel = level.getLevel();
        int currentLevel = level.getLevel();
        int currentExp = level.getExp();

        OptionalInt nextLevelExp = userGrowthPolicyService.getNextLevelExp(currentLevel);
        while (nextLevelExp.isPresent() && currentExp >= nextLevelExp.getAsInt()) {
            currentExp -= nextLevelExp.getAsInt();
            currentLevel += 1;
            nextLevelExp = userGrowthPolicyService.getNextLevelExp(currentLevel);
        }

        if (currentLevel != previousLevel) {
            level.advanceLevel(currentLevel, currentExp);
            userLevelHistoryRepository.save(new UserLevelHistory(
                    user,
                    previousLevel,
                    currentLevel,
                    reason
            ));
        }
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Inactive user account.");
        }
        return user;
    }
}
