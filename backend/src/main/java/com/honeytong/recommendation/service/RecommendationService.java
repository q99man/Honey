package com.honeytong.recommendation.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.recommendation.counter.RecommendationDailyCounter;
import com.honeytong.recommendation.dto.MyRecommendationResponse;
import com.honeytong.recommendation.dto.RecommendationPolicyResponse;
import com.honeytong.recommendation.dto.RecommendationResponse;
import com.honeytong.recommendation.entity.Recommendation;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import com.honeytong.user.service.UserActionLogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private static final String RECOMMEND_POLICY_GROUP = "recommend";
    private static final String DAILY_LIMIT_KEY = "daily_limit";

    private final RecommendationRepository recommendationRepository;
    private final PlaceRepository placeRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final PolicyService policyService;
    private final RecommendationDailyCounter recommendationDailyCounter;
    private final UserActionLogService userActionLogService;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            PlaceRepository placeRepository,
            PlaceStatsRepository placeStatsRepository,
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            PolicyService policyService,
            RecommendationDailyCounter recommendationDailyCounter,
            UserActionLogService userActionLogService
    ) {
        this.recommendationRepository = recommendationRepository;
        this.placeRepository = placeRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.policyService = policyService;
        this.recommendationDailyCounter = recommendationDailyCounter;
        this.userActionLogService = userActionLogService;
    }

    @Transactional
    public RecommendationResponse recommend(Long userId, Long placeId) {
        User user = getActiveUser(userId);
        Place place = getVisiblePlace(placeId);
        RecommendationPolicyResponse policy = getRecommendationPolicy(userId, placeId);
        if (!policy.canRecommend()) {
            throw toRecommendationPolicyException(policy.reason());
        }

        BigDecimal recommendWeight = getRecommendWeight(userId);
        Recommendation recommendation = recommendationRepository.findByUserIdAndPlaceId(userId, placeId)
                .map(existing -> {
                    existing.activate(recommendWeight);
                    return existing;
                })
                .orElseGet(() -> recommendationRepository.save(new Recommendation(user, place, recommendWeight)));

        PlaceStats stats = getStatsForUpdate(placeId);
        stats.addRecommendation(recommendation.getRecommendWeight());
        recommendationDailyCounter.evict(userId, LocalDate.now());
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_RECOMMENDATION_CREATE,
                UserActionLogService.TARGET_PLACE,
                place.getId(),
                Map.of(
                        "recommendWeight", recommendation.getRecommendWeight(),
                        "recommendationStatus", recommendation.getStatus().name()
                )
        );
        return new RecommendationResponse(true, stats.getRecommendCount(), recommendation.getRecommendWeight());
    }

    @Transactional
    public RecommendationResponse cancelRecommendation(Long userId, Long placeId) {
        User user = getActiveUser(userId);
        Place place = getVisiblePlace(placeId);
        Recommendation recommendation = recommendationRepository.findByUserIdAndPlaceId(userId, placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "추천 내역을 찾을 수 없습니다."));
        if (!recommendation.isActive()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 취소된 추천입니다.");
        }

        recommendation.cancel();
        PlaceStats stats = getStatsForUpdate(placeId);
        stats.removeRecommendation(recommendation.getRecommendWeight());
        recommendationDailyCounter.evict(userId, LocalDate.now());
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_RECOMMENDATION_CANCEL,
                UserActionLogService.TARGET_PLACE,
                place.getId(),
                Map.of(
                        "recommendWeight", recommendation.getRecommendWeight(),
                        "recommendationStatus", recommendation.getStatus().name()
                )
        );
        return new RecommendationResponse(false, stats.getRecommendCount(), recommendation.getRecommendWeight());
    }

    @Transactional(readOnly = true)
    public RecommendationPolicyResponse getRecommendationPolicy(Long userId, Long placeId) {
        getActiveUser(userId);
        getVisiblePlace(placeId);
        if (recommendationRepository.existsByUserIdAndPlaceIdAndStatus(
                userId,
                placeId,
                RecommendationStatus.ACTIVE
        )) {
            return new RecommendationPolicyResponse(false, "ALREADY_RECOMMENDED", getDailyRemainingCount(userId));
        }

        int dailyRemainingCount = getDailyRemainingCount(userId);
        if (dailyRemainingCount <= 0) {
            return new RecommendationPolicyResponse(false, "DAILY_LIMIT_EXCEEDED", 0);
        }

        return new RecommendationPolicyResponse(true, null, dailyRemainingCount);
    }

    @Transactional(readOnly = true)
    public List<MyRecommendationResponse> getMyRecommendations(Long userId) {
        getActiveUser(userId);
        return recommendationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, RecommendationStatus.ACTIVE)
                .stream()
                .map(recommendation -> new MyRecommendationResponse(
                        recommendation.getId(),
                        recommendation.getPlace().getId(),
                        recommendation.getPlace().getName(),
                        recommendation.getCreatedAt()
                ))
                .toList();
    }

    private int getDailyRemainingCount(Long userId) {
        int dailyLimit = policyService.getRequiredInteger(RECOMMEND_POLICY_GROUP, DAILY_LIMIT_KEY);
        if (dailyLimit < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "일일 추천 제한 정책은 0 이상이어야 합니다.");
        }

        LocalDate today = LocalDate.now();
        long usedCount = recommendationDailyCounter.getUsedCount(
                userId,
                today,
                () -> recommendationRepository.countByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        userId,
                        RecommendationStatus.ACTIVE,
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                )
        );
        return Math.max(0, dailyLimit - (int) usedCount);
    }

    private ApiException toRecommendationPolicyException(String reason) {
        if ("ALREADY_RECOMMENDED".equals(reason)) {
            return new ApiException(ErrorCode.RECOMMEND_ALREADY_EXISTS);
        }
        if ("DAILY_LIMIT_EXCEEDED".equals(reason)) {
            return new ApiException(ErrorCode.DAILY_RECOMMEND_LIMIT_EXCEEDED);
        }
        return new ApiException(ErrorCode.POLICY_VIOLATION);
    }

    private Place getVisiblePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        return place;
    }

    private PlaceStats getStatsForUpdate(Long placeId) {
        return placeStatsRepository.findByIdForUpdate(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소 통계를 찾을 수 없습니다."));
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private BigDecimal getRecommendWeight(Long userId) {
        return userTrustRepository.findById(userId)
                .map(UserTrust::getRecommendWeight)
                .orElse(BigDecimal.ONE);
    }
}
