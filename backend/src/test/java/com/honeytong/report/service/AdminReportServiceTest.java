package com.honeytong.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.honeytong.place.service.PlaceService;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.report.dto.AdminReportFollowUpActionRequest;
import com.honeytong.report.dto.AdminReportFollowUpActionType;
import com.honeytong.report.dto.AdminReportProcessRequest;
import com.honeytong.report.entity.Report;
import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.entity.ReportTargetType;
import com.honeytong.report.repository.ReportRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserSanctionType;
import com.honeytong.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long REPORTER_ID = 2L;
    private static final long REPORT_ID = 300L;
    private static final long TARGET_ID = 100L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private AdminPlaceService adminPlaceService;

    @Mock
    private AdminCommentService adminCommentService;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private PlaceService placeService;

    @Mock
    private PolicyService policyService;

    private AdminReportService adminReportService;
    private User admin;
    private User reporter;
    private Report report;

    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportService(
                reportRepository,
                userRepository,
                adminActionLogRepository,
                new ObjectMapper(),
                adminPlaceService,
                adminCommentService,
                adminUserService,
                placeService,
                policyService
        );

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        reporter = new User("reporter", "reporter@example.com");
        ReflectionTestUtils.setField(reporter, "id", REPORTER_ID);

        report = new Report(reporter, ReportTargetType.PLACE, TARGET_ID, "FRANCHISE", "Looks like a chain.");
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
    }

    @Test
    void getReports_returnsAllReportsWhenStatusIsNull() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(report));

        var response = adminReportService.getReports(ADMIN_ID, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).reportId()).isEqualTo(REPORT_ID);
        assertThat(response.get(0).reporterUserId()).isEqualTo(REPORTER_ID);
    }

    @Test
    void getReports_filtersByStatus() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING)).thenReturn(List.of(report));

        var response = adminReportService.getReports(ADMIN_ID, ReportStatus.PENDING);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void getReport_returnsDetail() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

        var response = adminReportService.getReport(ADMIN_ID, REPORT_ID);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.reasonCode()).isEqualTo("FRANCHISE");
    }

    @Test
    void processReport_updatesStatusAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        stubReviewNoteMaxLength(255);

        var response = adminReportService.processReport(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportProcessRequest(ReportStatus.APPROVED, "Confirmed.")
        );

        assertThat(response.status()).isEqualTo(ReportStatus.APPROVED);
        assertThat(response.reviewedByUserId()).isEqualTo(ADMIN_ID);
        assertThat(response.reviewNote()).isEqualTo("Confirmed.");
        assertThat(report.getReviewedBy()).isEqualTo(admin);
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void processReport_rejectsReviewNoteLongerThanPolicyLimit() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        stubReviewNoteMaxLength(5);

        assertThatThrownBy(() -> adminReportService.processReport(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportProcessRequest(ReportStatus.APPROVED, "123456")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void processReport_rejectsPendingStatus() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.processReport(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportProcessRequest(ReportStatus.PENDING, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void processReport_rejectsAlreadyProcessedReport() {
        report.process(admin, ReportStatus.REJECTED, "Not valid.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.processReport(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportProcessRequest(ReportStatus.APPROVED, "Confirmed.")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void getReports_rejectsNonAdmin() {
        User normalUser = new User("user", "user@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminReportService.getReports(9L, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void applyFollowUpAction_hidesPlaceForApprovedPlaceReport() {
        report.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        stubFollowUpMemoMaxLength(255);

        var response = adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.HIDE_PLACE,
                        null,
                        null,
                        null,
                        null,
                        "Hide reported place."
                )
        );

        assertThat(response.applied()).isTrue();
        assertThat(response.actionType()).isEqualTo(AdminReportFollowUpActionType.HIDE_PLACE);
        assertThat(response.resultTargetType()).isEqualTo("PLACE");
        verify(adminPlaceService).changeExposureStatus(
                any(Long.class),
                any(Long.class),
                any(AdminPlaceExposureStatusRequest.class)
        );
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void applyFollowUpAction_deletesPlaceForApprovedPlaceReport() {
        report.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        stubFollowUpMemoMaxLength(255);

        var response = adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.DELETE_PLACE,
                        null,
                        null,
                        null,
                        null,
                        "Delete reported place."
                )
        );

        assertThat(response.resultTargetType()).isEqualTo("PLACE");
        verify(placeService).deletePlace(ADMIN_ID, TARGET_ID);
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void applyFollowUpAction_blindsCommentForApprovedCommentReport() {
        Report commentReport = new Report(reporter, ReportTargetType.COMMENT, TARGET_ID, "ABUSE", "Offensive.");
        ReflectionTestUtils.setField(commentReport, "id", REPORT_ID);
        commentReport.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(commentReport));
        stubFollowUpMemoMaxLength(255);

        var response = adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.BLIND_COMMENT,
                        null,
                        null,
                        null,
                        null,
                        "Blind reported comment."
                )
        );

        assertThat(response.resultTargetType()).isEqualTo("COMMENT");
        verify(adminCommentService).blindComment(
                any(Long.class),
                any(Long.class),
                any(AdminCommentModerationRequest.class)
        );
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void applyFollowUpAction_sanctionsUserForApprovedUserReport() {
        Report userReport = new Report(reporter, ReportTargetType.USER, TARGET_ID, "ABUSE", "Repeated abuse.");
        ReflectionTestUtils.setField(userReport, "id", REPORT_ID);
        userReport.process(admin, ReportStatus.APPROVED, "Confirmed.");
        LocalDateTime endAt = LocalDateTime.now().plusDays(7);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(userReport));
        stubFollowUpReasonMaxLength(255);
        stubFollowUpMemoMaxLength(255);

        var response = adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.SANCTION_USER,
                        UserSanctionType.TEMPORARY_RESTRICTION,
                        "Confirmed report follow-up.",
                        null,
                        endAt,
                        "Restrict reported user."
                )
        );

        assertThat(response.resultTargetType()).isEqualTo("USER");
        verify(adminUserService).createSanction(
                any(Long.class),
                any(Long.class),
                any(AdminUserSanctionRequest.class)
        );
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void applyFollowUpAction_rejectsReasonLongerThanPolicyLimit() {
        Report userReport = new Report(reporter, ReportTargetType.USER, TARGET_ID, "ABUSE", "Repeated abuse.");
        ReflectionTestUtils.setField(userReport, "id", REPORT_ID);
        userReport.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(userReport));
        stubFollowUpReasonMaxLength(5);

        assertThatThrownBy(() -> adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.SANCTION_USER,
                        UserSanctionType.TEMPORARY_RESTRICTION,
                        "123456",
                        null,
                        LocalDateTime.now().plusDays(7),
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void applyFollowUpAction_rejectsContextMemoLongerThanPolicyLimit() {
        report.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        stubFollowUpMemoMaxLength(15);

        assertThatThrownBy(() -> adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.HIDE_PLACE,
                        null,
                        null,
                        null,
                        null,
                        "123456"
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void applyFollowUpAction_rejectsPendingReport() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.HIDE_PLACE,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void applyFollowUpAction_rejectsTargetTypeMismatch() {
        report.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.BLIND_COMMENT,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void applyFollowUpAction_requiresSanctionTypeForUserSanction() {
        Report userReport = new Report(reporter, ReportTargetType.USER, TARGET_ID, "ABUSE", "Repeated abuse.");
        ReflectionTestUtils.setField(userReport, "id", REPORT_ID);
        userReport.process(admin, ReportStatus.APPROVED, "Confirmed.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(userReport));

        assertThatThrownBy(() -> adminReportService.applyFollowUpAction(
                ADMIN_ID,
                REPORT_ID,
                new AdminReportFollowUpActionRequest(
                        AdminReportFollowUpActionType.SANCTION_USER,
                        null,
                        "Confirmed report follow-up.",
                        null,
                        null,
                        null
                )
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private void stubReviewNoteMaxLength(int maxLength) {
        when(policyService.getRequiredInteger("report", "review_note_max_length")).thenReturn(maxLength);
    }

    private void stubFollowUpReasonMaxLength(int maxLength) {
        when(policyService.getRequiredInteger("report", "follow_up_reason_max_length")).thenReturn(maxLength);
    }

    private void stubFollowUpMemoMaxLength(int maxLength) {
        when(policyService.getRequiredInteger("report", "follow_up_memo_max_length")).thenReturn(maxLength);
    }
}
