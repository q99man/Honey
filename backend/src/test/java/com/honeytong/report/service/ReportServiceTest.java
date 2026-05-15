package com.honeytong.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.report.dto.ReportCreateRequest;
import com.honeytong.report.entity.Report;
import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.entity.ReportTargetType;
import com.honeytong.report.repository.ReportRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final long REPORTER_ID = 1L;
    private static final long TARGET_USER_ID = 2L;
    private static final long PLACE_ID = 100L;
    private static final long COMMENT_ID = 200L;
    private static final long REPORT_ID = 300L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserActionLogService userActionLogService;

    @Mock
    private PolicyService policyService;

    private ReportService reportService;
    private User reporter;
    private User targetUser;
    private Place place;
    private Comment comment;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                userRepository,
                placeRepository,
                commentRepository,
                userActionLogService,
                policyService
        );

        reporter = new User("신고자", "reporter@example.com");
        ReflectionTestUtils.setField(reporter, "id", REPORTER_ID);
        targetUser = new User("대상자", "target@example.com");
        ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);

        RegionCity city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);

        place = new Place(
                reporter,
                dong,
                "서교 국밥",
                "KOREAN",
                "서울 마포구 양화로 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "국밥",
                "동네에서 다시 찾고 싶은 국밥집",
                "맑은 국물과 빠른 회전이 좋습니다.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);

        comment = new Comment(targetUser, place, "문제가 있는 댓글");
        ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
    }

    @Test
    void createReport_savesPlaceReport() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", REPORT_ID);
            return report;
        });
        stubReasonTextMaxLength(255);

        var response = reportService.createReport(
                REPORTER_ID,
                new ReportCreateRequest(ReportTargetType.PLACE, PLACE_ID, " franchise ", " 체인점으로 보입니다. ")
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        verify(reportRepository).save(any(Report.class));
        verify(userActionLogService).record(
                eq(REPORTER_ID),
                eq(UserActionLogService.ACTION_REPORT_CREATE),
                eq(UserActionLogService.TARGET_REPORT),
                eq(REPORT_ID),
                any()
        );
    }

    @Test
    void createReport_rejectsReasonTextLongerThanPolicyLimit() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        stubReasonTextMaxLength(5);

        assertThatThrownBy(() -> reportService.createReport(
                REPORTER_ID,
                new ReportCreateRequest(ReportTargetType.PLACE, PLACE_ID, "FAKE_INFO", "123456")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(reportRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createReport_allowsCommentReport() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", REPORT_ID);
            return report;
        });

        var response = reportService.createReport(
                REPORTER_ID,
                new ReportCreateRequest(ReportTargetType.COMMENT, COMMENT_ID, "abuse", null)
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
    }

    @Test
    void createReport_rejectsSelfUserReport() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));

        assertThatThrownBy(() -> reportService.createReport(
                REPORTER_ID,
                new ReportCreateRequest(ReportTargetType.USER, REPORTER_ID, "ABUSE", null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createReport_rejectsMissingTarget() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(
                REPORTER_ID,
                new ReportCreateRequest(ReportTargetType.PLACE, PLACE_ID, "FAKE_INFO", null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void stubReasonTextMaxLength(int maxLength) {
        when(policyService.getRequiredInteger("report", "reason_text_max_length")).thenReturn(maxLength);
    }

    @Test
    void getMyReports_returnsReporterReports() {
        Report report = new Report(reporter, ReportTargetType.USER, TARGET_USER_ID, "ABUSE", "욕설 신고");
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(REPORTER_ID)).thenReturn(List.of(report));

        var response = reportService.getMyReports(REPORTER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).reportId()).isEqualTo(REPORT_ID);
        assertThat(response.get(0).targetType()).isEqualTo(ReportTargetType.USER);
        assertThat(response.get(0).status()).isEqualTo(ReportStatus.PENDING);
    }
}
