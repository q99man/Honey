package com.honeytong.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.admin.dto.AdminUserDetailResponse;
import com.honeytong.admin.dto.AdminUserLevelResponse;
import com.honeytong.admin.dto.AdminUserListItemResponse;
import com.honeytong.admin.dto.AdminUserRecommendWeightRequest;
import com.honeytong.admin.dto.AdminUserSanctionRequest;
import com.honeytong.admin.dto.AdminUserSanctionResponse;
import com.honeytong.admin.dto.AdminUserTrustAdjustRequest;
import com.honeytong.admin.dto.AdminUserTrustAdjustResponse;
import com.honeytong.admin.dto.AdminUserTrustResponse;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserSanction;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserSanctionRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final String USER_TARGET_TYPE = "USER";
    private static final String USER_SANCTION_ACTION = "USER_SANCTION";
    private static final String USER_TRUST_ADJUST_ACTION = "USER_TRUST_ADJUST";
    private static final String USER_RECOMMEND_WEIGHT_ADJUST_ACTION = "USER_RECOMMEND_WEIGHT_ADJUST";
    private static final int RECOMMEND_WEIGHT_INTEGER_DIGITS = 2;
    private static final int RECOMMEND_WEIGHT_FRACTION_DIGITS = 2;

    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;
    private final AdminTextPolicyService adminTextPolicyService;

    public AdminUserService(
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            UserLevelRepository userLevelRepository,
            UserSanctionRepository userSanctionRepository,
            AdminActionLogRepository adminActionLogRepository,
            ObjectMapper objectMapper,
            AdminTextPolicyService adminTextPolicyService
    ) {
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.userLevelRepository = userLevelRepository;
        this.userSanctionRepository = userSanctionRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.objectMapper = objectMapper;
        this.adminTextPolicyService = adminTextPolicyService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItemResponse> getUsers(Long adminUserId) {
        ensureAdmin(adminUserId);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long adminUserId, Long userId) {
        ensureAdmin(adminUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        return toDetailResponse(user);
    }

    @Transactional
    public AdminUserSanctionResponse createSanction(
            Long adminUserId,
            Long userId,
            AdminUserSanctionRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        validateSanctionTarget(admin, targetUser);

        LocalDateTime startAt = request.startAt() == null ? LocalDateTime.now() : request.startAt();
        validateSanctionPeriod(startAt, request.endAt());

        UserTrust trust = userTrustRepository.findById(userId).orElse(null);
        String beforeValue = serializeSanctionState(targetUser, trust, null);

        UserSanction sanction = userSanctionRepository.save(new UserSanction(
                targetUser,
                request.sanctionType(),
                adminTextPolicyService.normalizeSanctionReason(request.reason()),
                startAt,
                request.endAt(),
                admin
        ));
        if (trust != null) {
            trust.increaseSanctionCount();
        }

        String afterValue = serializeSanctionState(targetUser, trust, sanction);
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                USER_SANCTION_ACTION,
                USER_TARGET_TYPE,
                targetUser.getId(),
                beforeValue,
                afterValue,
                adminTextPolicyService.normalizeActionMemo(request.memo())
        ));

        return toSanctionResponse(sanction);
    }

    @Transactional
    public AdminUserTrustAdjustResponse adjustTrust(
            Long adminUserId,
            Long userId,
            AdminUserTrustAdjustRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        User targetUser = getTrustAdjustmentTarget(admin, userId);
        UserTrust trust = getTrustOrThrow(userId);

        String beforeValue = serializeTrustState(targetUser, trust);
        trust.adjustTrust(request.trustScore(), request.trustGrade());
        String afterValue = serializeTrustState(targetUser, trust);

        adminActionLogRepository.save(new AdminActionLog(
                admin,
                USER_TRUST_ADJUST_ACTION,
                USER_TARGET_TYPE,
                targetUser.getId(),
                beforeValue,
                afterValue,
                adminTextPolicyService.normalizeActionMemo(request.memo())
        ));

        return toTrustAdjustResponse(targetUser, trust);
    }

    @Transactional
    public AdminUserTrustAdjustResponse adjustRecommendWeight(
            Long adminUserId,
            Long userId,
            AdminUserRecommendWeightRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        User targetUser = getTrustAdjustmentTarget(admin, userId);
        UserTrust trust = getTrustOrThrow(userId);
        BigDecimal recommendWeight = normalizeRecommendWeight(request.recommendWeight());

        String beforeValue = serializeTrustState(targetUser, trust);
        trust.adjustRecommendWeight(recommendWeight);
        String afterValue = serializeTrustState(targetUser, trust);

        adminActionLogRepository.save(new AdminActionLog(
                admin,
                USER_RECOMMEND_WEIGHT_ADJUST_ACTION,
                USER_TARGET_TYPE,
                targetUser.getId(),
                beforeValue,
                afterValue,
                adminTextPolicyService.normalizeActionMemo(request.memo())
        ));

        return toTrustAdjustResponse(targetUser, trust);
    }

    private void validateSanctionTarget(User admin, User targetUser) {
        if (!targetUser.isActive()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Only active users can be sanctioned.");
        }
        if (admin.getId().equals(targetUser.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Admins cannot sanction themselves.");
        }
        if (targetUser.getRole() != UserRole.USER) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Admin accounts cannot be sanctioned through this API.");
        }
    }

    private void validateSanctionPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt != null && !endAt.isAfter(startAt)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Sanction end time must be after start time.");
        }
    }

    private User getTrustAdjustmentTarget(User admin, Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        if (!targetUser.isActive()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Only active users can be adjusted.");
        }
        if (admin.getId().equals(targetUser.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Admins cannot adjust themselves.");
        }
        if (targetUser.getRole() != UserRole.USER) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Admin accounts cannot be adjusted through this API.");
        }
        return targetUser;
    }

    private UserTrust getTrustOrThrow(Long userId) {
        return userTrustRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User trust not found."));
    }

    private BigDecimal normalizeRecommendWeight(BigDecimal recommendWeight) {
        if (recommendWeight == null || recommendWeight.signum() < 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Recommendation weight must be zero or positive.");
        }
        if (recommendWeight.scale() > RECOMMEND_WEIGHT_FRACTION_DIGITS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Recommendation weight supports up to two decimal places.");
        }
        int integerDigits = recommendWeight.precision() - recommendWeight.scale();
        if (integerDigits > RECOMMEND_WEIGHT_INTEGER_DIGITS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Recommendation weight is too large.");
        }
        return recommendWeight.setScale(RECOMMEND_WEIGHT_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private AdminUserListItemResponse toListItemResponse(User user) {
        return new AdminUserListItemResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.isPhoneVerified(),
                user.getStatus(),
                user.getRole(),
                user.getLanguagePreference(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt()
        );
    }

    private AdminUserDetailResponse toDetailResponse(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getStatus(),
                user.getRole(),
                user.getLanguagePreference(),
                user.isMarketingAgreed(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt(),
                userTrustRepository.findById(user.getId()).map(this::toTrustResponse).orElse(null),
                userLevelRepository.findById(user.getId()).map(this::toLevelResponse).orElse(null)
        );
    }

    private AdminUserTrustResponse toTrustResponse(UserTrust trust) {
        return new AdminUserTrustResponse(
                trust.getTrustScore(),
                trust.getTrustGrade(),
                trust.getRecommendWeight(),
                trust.getSanctionCount(),
                trust.getReportReceivedCount(),
                trust.getReportConfirmedCount(),
                trust.getAbnormalActivityScore(),
                trust.isPhoneVerified(),
                trust.isRegionVerified(),
                trust.getLastEvaluatedAt()
        );
    }

    private AdminUserLevelResponse toLevelResponse(UserLevel level) {
        return new AdminUserLevelResponse(
                level.getLevel(),
                level.getExp(),
                level.getTotalExp(),
                level.getTitle(),
                level.getAvatarStage(),
                level.getRankScore()
        );
    }

    private AdminUserSanctionResponse toSanctionResponse(UserSanction sanction) {
        User user = sanction.getUser();
        User createdBy = sanction.getCreatedBy();
        return new AdminUserSanctionResponse(
                sanction.getId(),
                user.getId(),
                user.getNickname(),
                sanction.getSanctionType(),
                sanction.getReason(),
                sanction.getStartAt(),
                sanction.getEndAt(),
                sanction.getStatus(),
                createdBy.getId(),
                createdBy.getNickname(),
                sanction.getCreatedAt(),
                sanction.getUpdatedAt()
        );
    }

    private AdminUserTrustAdjustResponse toTrustAdjustResponse(User user, UserTrust trust) {
        return new AdminUserTrustAdjustResponse(
                user.getId(),
                user.getNickname(),
                trust.getTrustScore(),
                trust.getTrustGrade(),
                trust.getRecommendWeight(),
                trust.getLastEvaluatedAt()
        );
    }

    private String serializeSanctionState(User user, UserTrust trust, UserSanction sanction) {
        return serialize(Map.of(
                "userId", user.getId(),
                "status", user.getStatus().name(),
                "role", user.getRole().name(),
                "sanctionCount", trust == null ? "" : trust.getSanctionCount(),
                "sanctionId", sanction == null ? "" : sanction.getId(),
                "sanctionType", sanction == null ? "" : sanction.getSanctionType().name(),
                "sanctionStatus", sanction == null ? "" : sanction.getStatus().name()
        ));
    }

    private String serializeTrustState(User user, UserTrust trust) {
        return serialize(Map.of(
                "userId", user.getId(),
                "status", user.getStatus().name(),
                "role", user.getRole().name(),
                "trustScore", trust.getTrustScore(),
                "trustGrade", trust.getTrustGrade().name(),
                "recommendWeight", trust.getRecommendWeight(),
                "lastEvaluatedAt", trust.getLastEvaluatedAt() == null ? "" : trust.getLastEvaluatedAt().toString()
        ));
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Could not create admin action log.");
        }
    }

}
