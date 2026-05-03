package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.report.entity.ReportStatus;
import com.honeytong.report.repository.ReportRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.visit.repository.VisitRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PlaceRepository placeRepository;

    private AdminDashboardService adminDashboardService;
    private User admin;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T03:30:00Z"), ZONE);
        adminDashboardService = new AdminDashboardService(
                userRepository,
                recommendationRepository,
                visitRepository,
                reportRepository,
                placeRepository,
                clock
        );
        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
    }

    @Test
    void getDashboard_returnsTodayCounts() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 2, 0, 0);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(start, end))
                .thenReturn(3L);
        when(recommendationRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                RecommendationStatus.ACTIVE,
                start,
                end
        )).thenReturn(4L);
        when(visitRepository.countByValidTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end))
                .thenReturn(5L);
        when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(6L);
        when(placeRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(start, end))
                .thenReturn(7L);

        var response = adminDashboardService.getDashboard(ADMIN_ID);

        assertThat(response.todayNewUsers()).isEqualTo(3);
        assertThat(response.todayRecommendations()).isEqualTo(4);
        assertThat(response.todayVisits()).isEqualTo(5);
        assertThat(response.pendingReports()).isEqualTo(6);
        assertThat(response.newPlaces()).isEqualTo(7);
    }

    @Test
    void getDashboard_allowsSuperAdmin() {
        ReflectionTestUtils.setField(admin, "role", UserRole.SUPER_ADMIN);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        var response = adminDashboardService.getDashboard(ADMIN_ID);

        assertThat(response.pendingReports()).isZero();
    }

    @Test
    void getDashboard_rejectsNormalUser() {
        User user = new User("user", "user@example.com");
        ReflectionTestUtils.setField(user, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminDashboardService.getDashboard(9L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
