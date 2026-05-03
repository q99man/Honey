package com.honeytong.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminCommentModerationRequest;
import com.honeytong.admin.dto.AdminCommentModerationResponse;
import com.honeytong.admin.dto.AdminCommentResponse;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCommentService {

    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String COMMENT_WEIGHT_KEY = "comment_weight";
    private static final String COMMENT_TARGET_TYPE = "COMMENT";
    private static final String COMMENT_BLIND_ACTION = "COMMENT_BLIND";
    private static final String COMMENT_DELETE_ACTION = "COMMENT_DELETE";

    private final CommentRepository commentRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    public AdminCommentService(
            CommentRepository commentRepository,
            PlaceStatsRepository placeStatsRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            PolicyService policyService,
            ObjectMapper objectMapper
    ) {
        this.commentRepository = commentRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminCommentResponse> getComments(Long adminUserId) {
        ensureAdmin(adminUserId);
        return commentRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public AdminCommentModerationResponse blindComment(
            Long adminUserId,
            Long commentId,
            AdminCommentModerationRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Comment comment = getComment(commentId);
        PlaceStats stats = getStats(comment.getPlace().getId());
        if (comment.isVisible()) {
            String beforeValue = serializeCommentState(comment, stats);
            comment.blind();
            stats.removeComment(getCommentWeight());
            saveAdminLog(
                    admin,
                    COMMENT_BLIND_ACTION,
                    comment.getId(),
                    beforeValue,
                    serializeCommentState(comment, stats),
                    normalize(request == null ? null : request.memo())
            );
        }
        return toModerationResponse(comment, stats);
    }

    @Transactional
    public AdminCommentModerationResponse deleteComment(
            Long adminUserId,
            Long commentId,
            AdminCommentModerationRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Comment comment = getComment(commentId);
        PlaceStats stats = getStats(comment.getPlace().getId());
        if (comment.getStatus() != CommentStatus.DELETED) {
            String beforeValue = serializeCommentState(comment, stats);
            boolean wasVisible = comment.isVisible();
            comment.delete();
            if (wasVisible) {
                stats.removeComment(getCommentWeight());
            }
            saveAdminLog(
                    admin,
                    COMMENT_DELETE_ACTION,
                    comment.getId(),
                    beforeValue,
                    serializeCommentState(comment, stats),
                    normalize(request == null ? null : request.memo())
            );
        }
        return toModerationResponse(comment, stats);
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Comment not found."));
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place stats not found."));
    }

    private BigDecimal getCommentWeight() {
        BigDecimal commentWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, COMMENT_WEIGHT_KEY);
        if (commentWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "Comment score weight policy must be zero or greater.");
        }
        return commentWeight;
    }

    private void saveAdminLog(
            User admin,
            String actionType,
            Long targetId,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                actionType,
                COMMENT_TARGET_TYPE,
                targetId,
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializeCommentState(Comment comment, PlaceStats stats) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("commentId", comment.getId());
        value.put("placeId", comment.getPlace().getId());
        value.put("userId", comment.getUser().getId());
        value.put("status", comment.getStatus().name());
        value.put("commentCount", stats.getCommentCount());
        value.put("scoreTotal", stats.getScoreTotal());
        value.put("trustWeightedScore", stats.getTrustWeightedScore());
        value.put("deletedAt", comment.getDeletedAt() == null ? null : comment.getDeletedAt().toString());
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

    private AdminCommentResponse toCommentResponse(Comment comment) {
        User user = comment.getUser();
        Place place = comment.getPlace();
        return new AdminCommentResponse(
                comment.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                comment.getContent(),
                comment.getStatus(),
                comment.getReportCount(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getDeletedAt()
        );
    }

    private AdminCommentModerationResponse toModerationResponse(Comment comment, PlaceStats stats) {
        User user = comment.getUser();
        Place place = comment.getPlace();
        return new AdminCommentModerationResponse(
                comment.getId(),
                user.getId(),
                user.getNickname(),
                place.getId(),
                place.getName(),
                comment.getContent(),
                comment.getStatus(),
                stats.getCommentCount()
        );
    }
}
