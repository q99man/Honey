package com.honeytong.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminCommentModerationRequest;
import com.honeytong.admin.dto.AdminPlaceExposureStatusRequest;
import com.honeytong.admin.dto.AdminUserSanctionRequest;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.admin.service.AdminCommentService;
import com.honeytong.admin.service.AdminPlaceService;
import com.honeytong.admin.service.AdminUserService;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.service.PlaceService;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.report.dto.AdminReportFollowUpActionRequest;
import com.honeytong.report.dto.AdminReportFollowUpActionResponse;
import com.honeytong.report.dto.AdminReportFollowUpActionType;
import com.honeytong.report.dto.AdminReportProcessRequest;
import com.honeytong.report.dto.AdminReportResponse;
import com.honeytong.report.entity.Report;
import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.entity.ReportTargetType;
import com.honeytong.report.repository.ReportRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private static final String REPORT_TARGET_TYPE = "REPORT";
    private static final String REPORT_PROCESS_ACTION = "REPORT_PROCESS";
    private static final String REPORT_FOLLOW_UP_ACTION = "REPORT_FOLLOW_UP";
    private static final String REPORT_POLICY_GROUP = "report";
    private static final String REVIEW_NOTE_MAX_LENGTH_KEY = "review_note_max_length";
    private static final String FOLLOW_UP_REASON_MAX_LENGTH_KEY = "follow_up_reason_max_length";
    private static final String FOLLOW_UP_MEMO_MAX_LENGTH_KEY = "follow_up_memo_max_length";
    private static final int REVIEW_NOTE_COLUMN_LIMIT = 255;
    private static final int FOLLOW_UP_REASON_COLUMN_LIMIT = 255;
    private static final int FOLLOW_UP_MEMO_COLUMN_LIMIT = 255;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;
    private final AdminPlaceService adminPlaceService;
    private final AdminCommentService adminCommentService;
    private final AdminUserService adminUserService;
    private final PlaceService placeService;
    private final PolicyService policyService;

    public AdminReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            ObjectMapper objectMapper,
            AdminPlaceService adminPlaceService,
            AdminCommentService adminCommentService,
            AdminUserService adminUserService,
            PlaceService placeService,
            PolicyService policyService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.objectMapper = objectMapper;
        this.adminPlaceService = adminPlaceService;
        this.adminCommentService = adminCommentService;
        this.adminUserService = adminUserService;
        this.placeService = placeService;
        this.policyService = policyService;
    }

    @Transactional(readOnly = true)
    public List<AdminReportResponse> getReports(Long adminUserId, ReportStatus status) {
        ensureAdmin(adminUserId);
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getReport(Long adminUserId, Long reportId) {
        ensureAdmin(adminUserId);
        return toResponse(getReportOrThrow(reportId));
    }

    @Transactional
    public AdminReportResponse processReport(Long adminUserId, Long reportId, AdminReportProcessRequest request) {
        User admin = ensureAdmin(adminUserId);
        Report report = getReportOrThrow(reportId);
        validateProcessStatus(report, request.status());

        String beforeValue = serializeReport(report);
        report.process(admin, request.status(), normalizeReviewNote(request.reviewNote()));
        String afterValue = serializeReport(report);

        adminActionLogRepository.save(new AdminActionLog(
                admin,
                REPORT_PROCESS_ACTION,
                REPORT_TARGET_TYPE,
                report.getId(),
                beforeValue,
                afterValue,
                request.reviewNote()
        ));
        return toResponse(report);
    }

    @Transactional
    public AdminReportFollowUpActionResponse applyFollowUpAction(
            Long adminUserId,
            Long reportId,
            AdminReportFollowUpActionRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Report report = getReportOrThrow(reportId);
        validateFollowUpAction(report, request);

        String followUpReason = normalizeFollowUpReason(request.reason());
        String followUpMemo = normalize(request.memo());
        String actionMemo = memoWithReportContext(report, followUpMemo);
        validateFollowUpActionMemo(actionMemo);
        String resultTargetType;
        Long resultTargetId = report.getTargetId();
        switch (request.actionType()) {
            case HIDE_PLACE -> {
                adminPlaceService.changeExposureStatus(
                        adminUserId,
                        report.getTargetId(),
                        new AdminPlaceExposureStatusRequest(PlaceExposureStatus.HIDDEN, actionMemo)
                );
                resultTargetType = "PLACE";
            }
            case DELETE_PLACE -> {
                placeService.deletePlace(adminUserId, report.getTargetId());
                resultTargetType = "PLACE";
            }
            case BLIND_COMMENT -> {
                adminCommentService.blindComment(
                        adminUserId,
                        report.getTargetId(),
                        new AdminCommentModerationRequest(actionMemo)
                );
                resultTargetType = "COMMENT";
            }
            case DELETE_COMMENT -> {
                adminCommentService.deleteComment(
                        adminUserId,
                        report.getTargetId(),
                        new AdminCommentModerationRequest(actionMemo)
                );
                resultTargetType = "COMMENT";
            }
            case SANCTION_USER -> {
                adminUserService.createSanction(
                        adminUserId,
                        report.getTargetId(),
                        new AdminUserSanctionRequest(
                                request.sanctionType(),
                                followUpReason,
                                request.startAt(),
                                request.endAt(),
                                actionMemo
                        )
                );
                resultTargetType = "USER";
            }
            default -> throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported follow-up action.");
        }

        saveFollowUpLog(admin, report, request, followUpReason, followUpMemo, resultTargetType, resultTargetId);
        return new AdminReportFollowUpActionResponse(
                report.getId(),
                request.actionType(),
                report.getTargetType(),
                report.getTargetId(),
                resultTargetType,
                resultTargetId,
                true
        );
    }

    private void validateProcessStatus(Report report, ReportStatus status) {
        if (status == ReportStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Report processing status must be APPROVED or REJECTED.");
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Report has already been processed.");
        }
    }

    private void validateFollowUpAction(Report report, AdminReportFollowUpActionRequest request) {
        if (report.getStatus() != ReportStatus.APPROVED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Only approved reports can receive follow-up actions.");
        }
        ReportTargetType expectedTargetType = switch (request.actionType()) {
            case HIDE_PLACE, DELETE_PLACE -> ReportTargetType.PLACE;
            case BLIND_COMMENT, DELETE_COMMENT -> ReportTargetType.COMMENT;
            case SANCTION_USER -> ReportTargetType.USER;
        };
        if (report.getTargetType() != expectedTargetType) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Follow-up action does not match report target type.");
        }
        if (request.actionType() == AdminReportFollowUpActionType.SANCTION_USER && request.sanctionType() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Sanction type is required for user sanction actions.");
        }
    }

    private String normalizeReviewNote(String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            return null;
        }
        String normalized = reviewNote.trim();
        validatePolicyTextLength(
                normalized,
                REVIEW_NOTE_MAX_LENGTH_KEY,
                REVIEW_NOTE_COLUMN_LIMIT,
                "신고 검토 메모 허용 길이를 초과했습니다."
        );
        return normalized;
    }

    private String normalizeFollowUpReason(String reason) {
        String normalized = normalize(reason);
        if (normalized != null) {
            validatePolicyTextLength(
                    normalized,
                    FOLLOW_UP_REASON_MAX_LENGTH_KEY,
                    FOLLOW_UP_REASON_COLUMN_LIMIT,
                    "신고 후속 조치 사유 허용 길이를 초과했습니다."
            );
        }
        return normalized;
    }

    private void validateFollowUpActionMemo(String actionMemo) {
        validatePolicyTextLength(
                actionMemo,
                FOLLOW_UP_MEMO_MAX_LENGTH_KEY,
                FOLLOW_UP_MEMO_COLUMN_LIMIT,
                "신고 후속 조치 메모 허용 길이를 초과했습니다."
        );
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Report getReportOrThrow(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found."));
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private String serializeReport(Report report) {
        User reviewedBy = report.getReviewedBy();
        return serialize(Map.of(
                "reportId", report.getId(),
                "reporterUserId", report.getReporter().getId(),
                "targetType", report.getTargetType().name(),
                "targetId", report.getTargetId(),
                "reasonCode", report.getReasonCode(),
                "status", report.getStatus().name(),
                "reviewedByUserId", reviewedBy == null ? "" : reviewedBy.getId(),
                "reviewedAt", report.getReviewedAt() == null ? "" : report.getReviewedAt().toString(),
                "reviewNote", report.getReviewNote() == null ? "" : report.getReviewNote()
        ));
    }

    private void saveFollowUpLog(
            User admin,
            Report report,
            AdminReportFollowUpActionRequest request,
            String followUpReason,
            String followUpMemo,
            String resultTargetType,
            Long resultTargetId
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                REPORT_FOLLOW_UP_ACTION,
                REPORT_TARGET_TYPE,
                report.getId(),
                serializeReport(report),
                serializeFollowUp(report, request, followUpReason, resultTargetType, resultTargetId),
                followUpMemo
        ));
    }

    private String serializeFollowUp(
            Report report,
            AdminReportFollowUpActionRequest request,
            String followUpReason,
            String resultTargetType,
            Long resultTargetId
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("reportId", report.getId());
        value.put("actionType", request.actionType().name());
        value.put("reportTargetType", report.getTargetType().name());
        value.put("reportTargetId", report.getTargetId());
        value.put("resultTargetType", resultTargetType);
        value.put("resultTargetId", resultTargetId);
        value.put("sanctionType", request.sanctionType() == null ? null : request.sanctionType().name());
        value.put("reason", followUpReason);
        return serialize(value);
    }

    private String memoWithReportContext(Report report, String memo) {
        if (memo == null) {
            return "Report #" + report.getId() + " follow-up";
        }
        return "Report #" + report.getId() + ": " + memo;
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Could not create admin action log.");
        }
    }

    private AdminReportResponse toResponse(Report report) {
        User reporter = report.getReporter();
        User reviewedBy = report.getReviewedBy();
        return new AdminReportResponse(
                report.getId(),
                reporter.getId(),
                reporter.getNickname(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonCode(),
                report.getReasonText(),
                report.getStatus(),
                reviewedBy == null ? null : reviewedBy.getId(),
                reviewedBy == null ? null : reviewedBy.getNickname(),
                report.getReviewedAt(),
                report.getReviewNote(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
