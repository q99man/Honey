package com.honeytong.report.service;

import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.report.dto.MyReportResponse;
import com.honeytong.report.dto.ReportCreateRequest;
import com.honeytong.report.dto.ReportCreateResponse;
import com.honeytong.report.entity.Report;
import com.honeytong.report.entity.ReportTargetType;
import com.honeytong.report.repository.ReportRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final String REPORT_POLICY_GROUP = "report";
    private static final String REASON_TEXT_MAX_LENGTH_KEY = "reason_text_max_length";
    private static final int REASON_TEXT_COLUMN_LIMIT = 255;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final CommentRepository commentRepository;
    private final UserActionLogService userActionLogService;
    private final PolicyService policyService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            PlaceRepository placeRepository,
            CommentRepository commentRepository,
            UserActionLogService userActionLogService,
            PolicyService policyService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
        this.commentRepository = commentRepository;
        this.userActionLogService = userActionLogService;
        this.policyService = policyService;
    }

    @Transactional
    public ReportCreateResponse createReport(Long reporterUserId, ReportCreateRequest request) {
        User reporter = getActiveUser(reporterUserId);
        Long targetId = validateTarget(reporterUserId, request.targetType(), request.targetId());
        String reasonCode = normalizeReasonCode(request.reasonCode());
        String reasonText = normalizeReasonText(request.reasonText());

        Report report = reportRepository.save(new Report(
                reporter,
                request.targetType(),
                targetId,
                reasonCode,
                reasonText
        ));
        userActionLogService.record(
                reporter.getId(),
                UserActionLogService.ACTION_REPORT_CREATE,
                UserActionLogService.TARGET_REPORT,
                report.getId(),
                Map.of(
                        "reportTargetType", report.getTargetType().name(),
                        "reportTargetId", report.getTargetId(),
                        "reasonCode", report.getReasonCode()
                )
        );
        return new ReportCreateResponse(report.getId());
    }

    @Transactional(readOnly = true)
    public List<MyReportResponse> getMyReports(Long reporterUserId) {
        getActiveUser(reporterUserId);
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterUserId)
                .stream()
                .map(this::toMyReportResponse)
                .toList();
    }

    private Long validateTarget(Long reporterUserId, ReportTargetType targetType, Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "신고 대상이 올바르지 않습니다.");
        }
        return switch (targetType) {
            case PLACE -> validatePlaceTarget(targetId);
            case COMMENT -> validateCommentTarget(targetId);
            case USER -> validateUserTarget(reporterUserId, targetId);
        };
    }

    private Long validatePlaceTarget(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
        return place.getId();
    }

    private Long validateCommentTarget(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (!comment.isVisible()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
        return comment.getId();
    }

    private Long validateUserTarget(Long reporterUserId, Long targetUserId) {
        if (reporterUserId.equals(targetUserId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "자기 자신은 신고할 수 없습니다.");
        }
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다."));
        if (!targetUser.isActive()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "신고 대상을 찾을 수 없습니다.");
        }
        return targetUser.getId();
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private String normalizeReasonCode(String reasonCode) {
        String normalized = reasonCode == null ? "" : reasonCode.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "신고 사유 코드를 입력해 주세요.");
        }
        return normalized;
    }

    private String normalizeReasonText(String reasonText) {
        if (reasonText == null || reasonText.isBlank()) {
            return null;
        }
        String normalized = reasonText.trim();
        validatePolicyTextLength(
                normalized,
                REASON_TEXT_MAX_LENGTH_KEY,
                REASON_TEXT_COLUMN_LIMIT,
                "신고 상세 사유 허용 길이를 초과했습니다."
        );
        return normalized;
    }

    private void validatePolicyTextLength(
            String value,
            String policyKey,
            int columnLimit,
            String tooLongMessage
    ) {
        int maxLength = policyService.getRequiredInteger(REPORT_POLICY_GROUP, policyKey);
        if (maxLength <= 0 || maxLength > columnLimit) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "신고 텍스트 길이 정책은 1-" + columnLimit + " 사이여야 합니다."
            );
        }
        if (value.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, tooLongMessage);
        }
    }

    private MyReportResponse toMyReportResponse(Report report) {
        return new MyReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonCode(),
                report.getReasonText(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewedAt(),
                report.getReviewNote()
        );
    }
}
