package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminPlaceApprovalStatusRequest;
import com.honeytong.admin.dto.AdminPlaceExposureStatusRequest;
import com.honeytong.admin.dto.AdminPlaceFranchiseStatusRequest;
import com.honeytong.admin.dto.AdminPlaceScoreAdjustmentRequest;
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
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
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
class AdminPlaceServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long PLACE_ID = 100L;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private PlaceImageRepository placeImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    private AdminPlaceService adminPlaceService;
    private User admin;
    private User creator;
    private RegionDong dong;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        adminPlaceService = new AdminPlaceService(
                placeRepository,
                placeStatsRepository,
                placeImageRepository,
                userRepository,
                adminActionLogRepository,
                new ObjectMapper()
        );

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        creator = new User("bee", "bee@example.com");
        ReflectionTestUtils.setField(creator, "id", 2L);

        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);

        place = new Place(
                creator,
                dong,
                "Local Soup",
                "KOREAN",
                "Road address",
                "Jibun address",
                BigDecimal.valueOf(37.55),
                BigDecimal.valueOf(126.91),
                "10000_20000",
                "Soup",
                "Worth revisiting.",
                "Deep broth.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);
    }

    @Test
    void getPlaces_returnsAdminPlaceList() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(place));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminPlaceService.getPlaces(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().placeId()).isEqualTo(PLACE_ID);
        assertThat(response.getFirst().createdByUserId()).isEqualTo(2L);
        assertThat(response.getFirst().approvalStatus()).isEqualTo(place.getApprovalStatus());
        assertThat(response.getFirst().exposureStatus()).isEqualTo(place.getExposureStatus());
        assertThat(response.getFirst().franchiseReviewStatus()).isEqualTo(place.getFranchiseReviewStatus());
        assertThat(response.getFirst().rankingExcluded()).isFalse();
    }

    @Test
    void getPlace_returnsDetailWithStatsAndImages() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(PLACE_ID))
                .thenReturn(List.of(new PlaceImage(place, "https://image.example.com/place.jpg", 0, true, creator)));

        var response = adminPlaceService.getPlace(ADMIN_ID, PLACE_ID);

        assertThat(response.placeId()).isEqualTo(PLACE_ID);
        assertThat(response.name()).isEqualTo("Local Soup");
        assertThat(response.recommendCount()).isZero();
        assertThat(response.scoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.manualAdjustmentScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.rankingExcluded()).isFalse();
        assertThat(response.imageUrls()).containsExactly("https://image.example.com/place.jpg");
    }

    @Test
    void getPlace_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminPlaceService.getPlace(9L, PLACE_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void getPlace_returnsNotFoundForMissingPlace() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPlaceService.getPlace(ADMIN_ID, PLACE_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void changeExposureStatus_updatesStatusAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminPlaceService.changeExposureStatus(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceExposureStatusRequest(PlaceExposureStatus.HIDDEN, "Hide during review.")
        );

        assertThat(response.exposureStatus()).isEqualTo(PlaceExposureStatus.HIDDEN);
        assertThat(place.getExposureStatus()).isEqualTo(PlaceExposureStatus.HIDDEN);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void changeApprovalStatus_updatesStatusAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminPlaceService.changeApprovalStatus(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceApprovalStatusRequest(PlaceApprovalStatus.REJECTED, "Franchise rejected.")
        );

        assertThat(response.approvalStatus()).isEqualTo(PlaceApprovalStatus.REJECTED);
        assertThat(place.getApprovalStatus()).isEqualTo(PlaceApprovalStatus.REJECTED);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void changeFranchiseStatus_updatesStatusAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminPlaceService.changeFranchiseStatus(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceFranchiseStatusRequest(FranchiseReviewStatus.APPROVED, "Local shop confirmed.")
        );

        assertThat(response.franchiseReviewStatus()).isEqualTo(FranchiseReviewStatus.APPROVED);
        assertThat(place.getFranchiseReviewStatus()).isEqualTo(FranchiseReviewStatus.APPROVED);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void changeExposureStatus_doesNotLogWhenStatusIsUnchanged() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        var response = adminPlaceService.changeExposureStatus(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceExposureStatusRequest(PlaceExposureStatus.VISIBLE, "No-op.")
        );

        assertThat(response.exposureStatus()).isEqualTo(PlaceExposureStatus.VISIBLE);
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void adjustScore_updatesManualAdjustmentAndLogsAction() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminPlaceService.adjustScore(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceScoreAdjustmentRequest(BigDecimal.valueOf(1.25), "Boost after review.")
        );

        assertThat(response.scoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.manualAdjustmentScore()).isEqualByComparingTo("1.25");
        assertThat(stats.getManualAdjustmentScore()).isEqualByComparingTo("1.25");
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void adjustScore_rejectsZeroDelta() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        assertThatThrownBy(() -> adminPlaceService.adjustScore(
                ADMIN_ID,
                PLACE_ID,
                new AdminPlaceScoreAdjustmentRequest(BigDecimal.ZERO, "No score change.")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }
}
