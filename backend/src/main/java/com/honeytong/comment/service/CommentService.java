package com.honeytong.comment.service;

import com.honeytong.comment.dto.CommentCreateResponse;
import com.honeytong.comment.dto.CommentDeleteResponse;
import com.honeytong.comment.dto.CommentRequest;
import com.honeytong.comment.dto.CommentResponse;
import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.event.CommentCreatedEvent;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.mission.entity.MissionTargetType;
import com.honeytong.mission.service.MissionService;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.honeytong.fraud.service.FraudDetectionService;

@Service
public class CommentService {

    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String COMMENT_POLICY_GROUP = "comment";
    private static final String COMMENT_WEIGHT_KEY = "comment_weight";
    private static final String MAX_LENGTH_KEY = "max_length";
    private static final int COMMENT_CONTENT_COLUMN_LIMIT = 300;

    private final CommentRepository commentRepository;
    private final PlaceRepository placeRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final UserRepository userRepository;
    private final PolicyService policyService;
    private final UserActionLogService userActionLogService;
    private final MissionService missionService;
    private final ApplicationEventPublisher eventPublisher;
    private final FraudDetectionService fraudDetectionService;

    public CommentService(
            CommentRepository commentRepository,
            PlaceRepository placeRepository,
            PlaceStatsRepository placeStatsRepository,
            UserRepository userRepository,
            PolicyService policyService,
            UserActionLogService userActionLogService,
            MissionService missionService,
            ApplicationEventPublisher eventPublisher,
            FraudDetectionService fraudDetectionService
    ) {
        this.commentRepository = commentRepository;
        this.placeRepository = placeRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.userRepository = userRepository;
        this.policyService = policyService;
        this.userActionLogService = userActionLogService;
        this.missionService = missionService;
        this.eventPublisher = eventPublisher;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public CommentCreateResponse createComment(Long userId, Long placeId, CommentRequest request, String clientIp) {
        User user = getActiveUser(userId);
        Place place = getVisiblePlace(placeId);
        String content = normalizeContent(request.content());

        Comment comment = commentRepository.findByUserIdAndPlaceId(userId, placeId)
                .map(existing -> restoreDeletedComment(existing, content))
                .orElseGet(() -> commentRepository.save(new Comment(user, place, content)));

        PlaceStats stats = getStatsForUpdate(placeId);
        stats.addComment(getCommentWeight());
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMENT_CREATE,
                UserActionLogService.TARGET_COMMENT,
                comment.getId(),
                Map.of("placeId", place.getId())
        );
        missionService.trackProgress(userId, MissionTargetType.COMMENT);

        eventPublisher.publishEvent(new CommentCreatedEvent(
                comment.getId(),
                place.getId(),
                user.getId(),
                place.getCreatedBy().getId(),
                place.getName(),
                comment.getContent()
        ));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fraudDetectionService.auditUserAction(userId, UserActionLogService.ACTION_COMMENT_CREATE, clientIp, comment.getId());
                }
            });
        } else {
            fraudDetectionService.auditUserAction(userId, UserActionLogService.ACTION_COMMENT_CREATE, clientIp, comment.getId());
        }

        return new CommentCreateResponse(comment.getId());
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest request) {
        User user = getActiveUser(userId);
        Comment comment = getVisibleComment(commentId);
        validateOwner(userId, comment);
        comment.updateContent(normalizeContent(request.content()));
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMENT_UPDATE,
                UserActionLogService.TARGET_COMMENT,
                comment.getId(),
                Map.of("placeId", comment.getPlace().getId())
        );
        return toCommentResponse(comment);
    }

    @Transactional
    public CommentDeleteResponse deleteComment(Long userId, Long commentId) {
        User user = getActiveUser(userId);
        Comment comment = getVisibleComment(commentId);
        validateOwner(userId, comment);
        comment.delete();

        PlaceStats stats = getStatsForUpdate(comment.getPlace().getId());
        stats.removeComment(getCommentWeight());
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMENT_DELETE,
                UserActionLogService.TARGET_COMMENT,
                comment.getId(),
                Map.of("placeId", comment.getPlace().getId())
        );
        return new CommentDeleteResponse(comment.getId(), true, stats.getCommentCount());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getPlaceComments(Long placeId) {
        getVisiblePlace(placeId);
        return commentRepository.findByPlaceIdAndStatusOrderByCreatedAtDesc(placeId, CommentStatus.VISIBLE)
                .stream()
                .filter(Comment::isVisible)
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getMyComments(Long userId) {
        getActiveUser(userId);
        return commentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, CommentStatus.VISIBLE)
                .stream()
                .filter(Comment::isVisible)
                .map(this::toCommentResponse)
                .toList();
    }

    private Comment restoreDeletedComment(Comment comment, String content) {
        if (comment.isVisible()) {
            throw new ApiException(ErrorCode.COMMENT_ALREADY_EXISTS);
        }
        if (comment.getStatus() != CommentStatus.DELETED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "해당 댓글을 다시 작성할 수 없습니다.");
        }
        comment.restore(content);
        return comment;
    }

    private void validateOwner(Long userId, Comment comment) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "댓글 작성자만 변경할 수 있습니다.");
        }
    }

    private BigDecimal getCommentWeight() {
        BigDecimal commentWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, COMMENT_WEIGHT_KEY);
        if (commentWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "댓글 점수 가중치 정책은 0 이상이어야 합니다.");
        }
        return commentWeight;
    }

    private Place getVisiblePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        return place;
    }

    private Comment getVisibleComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!comment.isVisible()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        return comment;
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

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "댓글 내용을 입력해 주세요.");
        }
        int maxLength = getMaxContentLength();
        if (normalized.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "댓글 내용 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    private int getMaxContentLength() {
        int maxLength = policyService.getRequiredInteger(COMMENT_POLICY_GROUP, MAX_LENGTH_KEY);
        if (maxLength <= 0 || maxLength > COMMENT_CONTENT_COLUMN_LIMIT) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "댓글 길이 정책은 1-" + COMMENT_CONTENT_COLUMN_LIMIT + " 사이여야 합니다."
            );
        }
        return maxLength;
    }

    private CommentResponse toCommentResponse(Comment comment) {
        Place place = comment.getPlace();
        User user = comment.getUser();
        return new CommentResponse(
                comment.getId(),
                place.getId(),
                place.getName(),
                user.getId(),
                user.getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
