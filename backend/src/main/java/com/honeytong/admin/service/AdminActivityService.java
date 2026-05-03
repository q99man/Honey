package com.honeytong.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminActivityInvalidationRequest;
import com.honeytong.admin.dto.AdminRecommendationInvalidationResponse;
import com.honeytong.admin.dto.AdminRecommendationResponse;
import com.honeytong.admin.dto.AdminVisitInvalidationResponse;
import com.honeytong.admin.dto.AdminVisitResponse;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.recommendation.counter.RecommendationDailyCounter;
import com.honeytong.recommendation.entity.Recommendation;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.cooldown.VisitCooldownCache;
import com.honeytong.visit.repository.VisitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActivityService {

    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String VISIT_WEIGHT_KEY = "visit_weight";
    private static final String RECOMMENDATION_TARGET_TYPE = "RECOMMENDATION";
    private static final String VISIT_TARGET_TYPE = "VISIT";
    private static final String RECOMMENDATION_INVALIDATE_ACTION = "RECOMMENDATION_INVALIDATE";
    private static final String VISIT_INVALIDATE_ACTION = "VISIT_INVALIDATE";
    private static final String ADMIN_INVALIDATED_VISIT_REASON = "ADMIN_INVALIDATED";

    private final RecommendationRepository recommendationRepository;
    private final VisitRepository visitRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PolicyService policyService;
    private final RecommendationDailyCounter recommendationDailyCounter;
    private final VisitCooldownCache visitCooldownCache;
    private final ObjectMapper objectMapper;

    public AdminActivityService(
            RecommendationRepository recommendationRepository,
            VisitRepository visitRepository,
            PlaceStatsRepository placeStatsRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            PolicyService policyService,
            RecommendationDailyCounter recommendationDailyCounter,
            VisitCooldownCache visitCooldownCache,
            ObjectMapper objectMapper
    ) {
        this.recommendationRepository = recommendationRepository;
        this.visitRepository = visitRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.policyService = policyService;
        this.recommendationDailyCounter = recommendationDailyCounter;
        this.visitCooldownCache = visitCooldownCache;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminRecommendationResponse> getRecommendations(Long adminUserId) {
        ensureAdmin(adminUserId);
        return recommendationRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toRecommendationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminVisitResponse> getVisits(Long adminUserId) {
        ensureAdmin(adminUserId);
        return visitRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toVisitResponse)
                .toList();
    }

    @Transactional
    public AdminRecommendationInvalidationResponse invalidateRecommendation(
            Long adminUserId,
            Long recommendationId,
            AdminActivityInvalidationRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Recommendation not found."));
        Place place = recommendation.getPlace();
        PlaceStats stats = getStats(place.getId());
        if (recommendation.isActive()) {
            String beforeValue = serializeRecommendationState(recommendation, stats);
            recommendation.invalidate();
            stats.removeRecommendation(recommendation.getRecommendWeight());
            recommendationDailyCounter.evict(recommendation.getUser().getId(), LocalDate.now());
            saveAdminLog(
                    admin,
                    RECOMMENDATION_INVALIDATE_ACTION,
                    RECOMMENDATION_TARGET_TYPE,
                    recommendation.getId(),
                    beforeValue,
                    serializeRecommendationState(recommendation, stats),
                    normalize(request == null ? null : request.memo())
            );
        }
        return toRecommendationInvalidationResponse(recommendation, stats);
    }

    @Transactional
    public AdminVisitInvalidationResponse invalidateVisit(
            Long adminUserId,
            Long visitId,
            AdminActivityInvalidationRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Visit not found."));
        Place place = visit.getPlace();
        PlaceStats stats = getStats(place.getId());
        if (visit.isValid()) {
            String beforeValue = serializeVisitState(visit, stats);
            visit.invalidate(ADMIN_INVALIDATED_VISIT_REASON);
            stats.removeVisit(getVisitWeight());
            visitCooldownCache.evict(visit.getUser().getId(), place.getId());
            saveAdminLog(
                    admin,
                    VISIT_INVALIDATE_ACTION,
                    VISIT_TARGET_TYPE,
                    visit.getId(),
                    beforeValue,
                    serializeVisitState(visit, stats),
                    normalize(request == null ? null : request.memo())
            );
        }
        return toVisitInvalidationResponse(visit, stats);
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place stats not found."));
    }

    private BigDecimal getVisitWeight() {
        BigDecimal visitWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, VISIT_WEIGHT_KEY);
        if (visitWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "Visit score weight policy must be zero or greater.");
        }
        return visitWeight;
    }

    private void saveAdminLog(
            User admin,
            String actionType,
            String targetType,
            Long targetId,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                actionType,
                targetType,
                targetId,
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializeRecommendationState(Recommendation recommendation, PlaceStats stats) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("recommendationId", recommendation.getId());
        value.put("placeId", recommendation.getPlace().getId());
        value.put("status", recommendation.getStatus().name());
        value.put("recommendWeight", recommendation.getRecommendWeight());
        value.put("recommendCount", stats.getRecommendCount());
        value.put("scoreTotal", stats.getScoreTotal());
        value.put("trustWeightedScore", stats.getTrustWeightedScore());
        return serialize(value);
    }

    private String serializeVisitState(Visit visit, PlaceStats stats) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("visitId", visit.getId());
        value.put("placeId", visit.getPlace().getId());
        value.put("valid", visit.isValid());
        value.put("validReason", visit.getValidReason());
        value.put("visitCount", stats.getVisitCount());
        value.put("scoreTotal", stats.getScoreTotal());
        value.put("trustWeightedScore", stats.getTrustWeightedScore());
        return serialize(value);
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Could not create admin action log.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AdminRecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        User user = recommendation.getUser();
        Place place = recommendation.getPlace();
        return new AdminRecommendationResponse(
                recommendation.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                recommendation.getStatus(),
                recommendation.getRecommendWeight(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }

    private AdminVisitResponse toVisitResponse(Visit visit) {
        User user = visit.getUser();
        Place place = visit.getPlace();
        return new AdminVisitResponse(
                visit.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                visit.getLatitude(),
                visit.getLongitude(),
                visit.getDistanceMeter(),
                visit.getImageUrl(),
                visit.isValid(),
                visit.getValidReason(),
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }

    private AdminRecommendationInvalidationResponse toRecommendationInvalidationResponse(
            Recommendation recommendation,
            PlaceStats stats
    ) {
        User user = recommendation.getUser();
        Place place = recommendation.getPlace();
        return new AdminRecommendationInvalidationResponse(
                recommendation.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                recommendation.getStatus(),
                stats.getRecommendCount()
        );
    }

    private AdminVisitInvalidationResponse toVisitInvalidationResponse(Visit visit, PlaceStats stats) {
        User user = visit.getUser();
        Place place = visit.getPlace();
        return new AdminVisitInvalidationResponse(
                visit.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                visit.isValid(),
                visit.getValidReason(),
                stats.getVisitCount()
        );
    }
}
