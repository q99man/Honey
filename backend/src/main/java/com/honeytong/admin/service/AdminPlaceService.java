package com.honeytong.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminPlaceApprovalStatusRequest;
import com.honeytong.admin.dto.AdminPlaceDetailResponse;
import com.honeytong.admin.dto.AdminPlaceExposureStatusRequest;
import com.honeytong.admin.dto.AdminPlaceFranchiseStatusRequest;
import com.honeytong.admin.dto.AdminPlaceListItemResponse;
import com.honeytong.admin.dto.AdminPlaceModerationResponse;
import com.honeytong.admin.dto.AdminPlaceScoreAdjustmentRequest;
import com.honeytong.admin.dto.AdminPlaceScoreAdjustmentResponse;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.FranchiseReviewStatus;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceApprovalStatus;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceImage;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceImageRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPlaceService {

    private static final String PLACE_TARGET_TYPE = "PLACE";
    private static final String PLACE_EXPOSURE_ACTION = "PLACE_EXPOSURE_UPDATE";
    private static final String PLACE_APPROVAL_ACTION = "PLACE_APPROVAL_UPDATE";
    private static final String PLACE_FRANCHISE_REVIEW_ACTION = "PLACE_FRANCHISE_REVIEW_UPDATE";
    private static final String PLACE_SCORE_ADJUST_ACTION = "PLACE_SCORE_ADJUST";

    private final PlaceRepository placeRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final PlaceImageRepository placeImageRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;

    public AdminPlaceService(
            PlaceRepository placeRepository,
            PlaceStatsRepository placeStatsRepository,
            PlaceImageRepository placeImageRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            ObjectMapper objectMapper
    ) {
        this.placeRepository = placeRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.placeImageRepository = placeImageRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminPlaceListItemResponse> getPlaces(Long adminUserId) {
        ensureAdmin(adminUserId);
        return placeRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(place -> toListItemResponse(place, getStats(place.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPlaceDetailResponse getPlace(Long adminUserId, Long placeId) {
        ensureAdmin(adminUserId);
        Place place = placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found."));
        PlaceStats stats = getStats(placeId);
        List<String> imageUrls = placeImageRepository.findByPlaceIdOrderBySortOrderAsc(placeId).stream()
                .map(PlaceImage::getImageUrl)
                .toList();
        return toDetailResponse(place, stats, imageUrls);
    }

    @Transactional
    public AdminPlaceModerationResponse changeExposureStatus(
            Long adminUserId,
            Long placeId,
            AdminPlaceExposureStatusRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Place place = getPlaceOrThrow(placeId);
        PlaceExposureStatus beforeStatus = place.getExposureStatus();
        if (beforeStatus != request.exposureStatus()) {
            String beforeValue = serializePlaceModerationState(place);
            place.changeExposureStatus(request.exposureStatus());
            saveActionLog(admin, place, PLACE_EXPOSURE_ACTION, beforeValue, normalize(request.memo()));
        }
        return toModerationResponse(place);
    }

    @Transactional
    public AdminPlaceModerationResponse changeApprovalStatus(
            Long adminUserId,
            Long placeId,
            AdminPlaceApprovalStatusRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Place place = getPlaceOrThrow(placeId);
        PlaceApprovalStatus beforeStatus = place.getApprovalStatus();
        if (beforeStatus != request.approvalStatus()) {
            String beforeValue = serializePlaceModerationState(place);
            place.changeApprovalStatus(request.approvalStatus());
            saveActionLog(admin, place, PLACE_APPROVAL_ACTION, beforeValue, normalize(request.memo()));
        }
        return toModerationResponse(place);
    }

    @Transactional
    public AdminPlaceModerationResponse changeFranchiseStatus(
            Long adminUserId,
            Long placeId,
            AdminPlaceFranchiseStatusRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Place place = getPlaceOrThrow(placeId);
        FranchiseReviewStatus beforeStatus = place.getFranchiseReviewStatus();
        if (beforeStatus != request.franchiseReviewStatus()) {
            String beforeValue = serializePlaceModerationState(place);
            place.changeFranchiseReviewStatus(request.franchiseReviewStatus());
            saveActionLog(admin, place, PLACE_FRANCHISE_REVIEW_ACTION, beforeValue, normalize(request.memo()));
        }
        return toModerationResponse(place);
    }

    @Transactional
    public AdminPlaceScoreAdjustmentResponse adjustScore(
            Long adminUserId,
            Long placeId,
            AdminPlaceScoreAdjustmentRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Place place = getPlaceOrThrow(placeId);
        PlaceStats stats = getStatsForUpdate(placeId);
        BigDecimal scoreDelta = normalizeScoreDelta(request.scoreDelta());
        if (scoreDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Score delta must not be zero.");
        }

        String beforeValue = serializePlaceScoreState(place, stats);
        stats.adjustManualScore(scoreDelta);
        saveActionLog(
                admin,
                place,
                PLACE_SCORE_ADJUST_ACTION,
                beforeValue,
                serializePlaceScoreState(place, stats),
                normalize(request.memo())
        );
        return new AdminPlaceScoreAdjustmentResponse(
                place.getId(),
                place.getName(),
                stats.getScoreTotal(),
                stats.getManualAdjustmentScore()
        );
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private Place getPlaceOrThrow(Long placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found."));
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place stats not found."));
    }

    private PlaceStats getStatsForUpdate(Long placeId) {
        return placeStatsRepository.findByIdForUpdate(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place stats not found."));
    }

    private void saveActionLog(User admin, Place place, String actionType, String beforeValue, String memo) {
        saveActionLog(admin, place, actionType, beforeValue, serializePlaceModerationState(place), memo);
    }

    private void saveActionLog(
            User admin,
            Place place,
            String actionType,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                actionType,
                PLACE_TARGET_TYPE,
                place.getId(),
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializePlaceModerationState(Place place) {
        return serialize(Map.of(
                "placeId", place.getId(),
                "approvalStatus", place.getApprovalStatus().name(),
                "exposureStatus", place.getExposureStatus().name(),
                "franchiseReviewStatus", place.getFranchiseReviewStatus().name()
        ));
    }

    private String serializePlaceScoreState(Place place, PlaceStats stats) {
        return serialize(Map.of(
                "placeId", place.getId(),
                "scoreTotal", stats.getScoreTotal(),
                "manualAdjustmentScore", stats.getManualAdjustmentScore()
        ));
    }

    private BigDecimal normalizeScoreDelta(BigDecimal scoreDelta) {
        try {
            return scoreDelta.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Score delta must use at most two decimal places.");
        }
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

    private AdminPlaceModerationResponse toModerationResponse(Place place) {
        return new AdminPlaceModerationResponse(
                place.getId(),
                place.getName(),
                place.getApprovalStatus(),
                place.getExposureStatus(),
                place.getFranchiseReviewStatus()
        );
    }

    private AdminPlaceListItemResponse toListItemResponse(Place place, PlaceStats stats) {
        User createdBy = place.getCreatedBy();
        return new AdminPlaceListItemResponse(
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                createdBy.getId(),
                createdBy.getNickname(),
                place.getRegionCity().getId(),
                place.getRegionDistrict().getId(),
                place.getRegionDong().getId(),
                place.getRegionCity().getNameKo(),
                place.getRegionDistrict().getNameKo(),
                place.getRegionDong().getNameKo(),
                place.isFranchise(),
                place.getFranchiseReviewStatus(),
                place.getApprovalStatus(),
                place.getExposureStatus(),
                place.isRankingExcluded(),
                place.getCurrentStarLevel(),
                place.getCurrentFlowerGrade(),
                stats.getRecommendCount(),
                stats.getVisitCount(),
                stats.getCommentCount(),
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }

    private AdminPlaceDetailResponse toDetailResponse(Place place, PlaceStats stats, List<String> imageUrls) {
        User createdBy = place.getCreatedBy();
        return new AdminPlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                createdBy.getId(),
                createdBy.getNickname(),
                place.getRegionCity().getId(),
                place.getRegionDistrict().getId(),
                place.getRegionDong().getId(),
                place.getRegionCity().getNameKo(),
                place.getRegionDistrict().getNameKo(),
                place.getRegionDong().getNameKo(),
                place.getAddressRoad(),
                place.getAddressJibun(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceRangeCode(),
                place.getRecommendedMenu(),
                place.getShortRecommendation(),
                place.getFeatureText(),
                place.isFranchise(),
                place.getFranchiseReviewStatus(),
                place.getApprovalStatus(),
                place.getExposureStatus(),
                place.isRankingExcluded(),
                place.getCurrentStarLevel(),
                place.getCurrentFlowerGrade(),
                stats.getRecommendCount(),
                stats.getVisitCount(),
                stats.getCommentCount(),
                stats.getUniqueUserCount(),
                stats.getScoreTotal(),
                stats.getManualAdjustmentScore(),
                stats.getRecentScore(),
                stats.getDiversityScore(),
                stats.getTrustWeightedScore(),
                stats.getLastActivityAt(),
                imageUrls,
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }
}
