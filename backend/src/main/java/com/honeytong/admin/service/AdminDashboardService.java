package com.honeytong.admin.service;

import com.honeytong.admin.dto.AdminDashboardResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;
    private final VisitRepository visitRepository;
    private final ReportRepository reportRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    @Autowired
    public AdminDashboardService(
            UserRepository userRepository,
            RecommendationRepository recommendationRepository,
            VisitRepository visitRepository,
            ReportRepository reportRepository,
            PlaceRepository placeRepository
    ) {
        this(userRepository, recommendationRepository, visitRepository, reportRepository, placeRepository, Clock.systemDefaultZone());
    }

    AdminDashboardService(
            UserRepository userRepository,
            RecommendationRepository recommendationRepository,
            VisitRepository visitRepository,
            ReportRepository reportRepository,
            PlaceRepository placeRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
        this.visitRepository = visitRepository;
        this.reportRepository = reportRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(Long adminUserId) {
        ensureAdmin(adminUserId);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        return new AdminDashboardResponse(
                userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
                        startOfToday,
                        startOfTomorrow
                ),
                recommendationRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        RecommendationStatus.ACTIVE,
                        startOfToday,
                        startOfTomorrow
                ),
                visitRepository.countByValidTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        startOfToday,
                        startOfTomorrow
                ),
                reportRepository.countByStatus(ReportStatus.PENDING),
                placeRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
                        startOfToday,
                        startOfTomorrow
                )
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
}
