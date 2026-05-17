package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.place.dto.PlaceUpdateRequest;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceImage;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceImageRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.entity.UserRegionStatus;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.repository.UserRegionRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserSanctionRepository;
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
class PlaceServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceImageRepository placeImageRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private PlaceSearchDocumentService placeSearchDocumentService;

    @Mock
    private RegionDongRepository regionDongRepository;

    @Mock
    private UserRegionRepository userRegionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private UserActionLogService userActionLogService;

    private PlaceService placeService;
    private User user;
    private RegionCity city;
    private RegionDistrict district;
    private RegionDong userDong;
    private RegionDong targetDong;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(
                placeRepository,
                placeImageRepository,
                placeStatsRepository,
                placeSearchDocumentService,
                regionDongRepository,
                userRegionRepository,
                userRepository,
                userSanctionRepository,
                policyService,
                adminActionLogRepository,
                new ObjectMapper(),
                userActionLogService
        );

        user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.verifyPhone("01012345678");
        city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        userDong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(userDong, "id", 30L);
        targetDong = new RegionDong(city, district, "Hapjeong-dong", "Hapjeong-dong", null, "1144068000");
        ReflectionTestUtils.setField(targetDong, "id", 31L);
    }

    @Test
    void createPlace_savesPlaceImagesAndStatsWhenPolicyAllows() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            ReflectionTestUtils.setField(place, "id", 100L);
            return place;
        });

        var response = placeService.createPlace(USER_ID, createRequest());

        assertThat(response.placeId()).isEqualTo(100L);
        verify(placeImageRepository).save(any(PlaceImage.class));
        verify(placeStatsRepository).save(any(PlaceStats.class));
        verify(placeSearchDocumentService).syncPlace(any(Place.class));
        verify(userActionLogService).record(
                eq(USER_ID),
                eq(UserActionLogService.ACTION_PLACE_CREATE),
                eq(UserActionLogService.TARGET_PLACE),
                eq(100L),
                any()
        );
    }

    @Test
    void createPlace_rejectsShortRecommendationLongerThanPolicyLimit() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("DISTRICT");
        when(policyService.getRequiredInteger("place", "recommended_menu_max_length")).thenReturn(255);
        when(policyService.getRequiredInteger("place", "short_recommendation_max_length")).thenReturn(5);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(
                USER_ID,
                createRequestWithText("Test Menu", "123456", "Fast service")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(placeRepository, never()).save(any());
        verify(placeStatsRepository, never()).save(any());
    }

    @Test
    void createPlace_rejectsPlaceOutsideRegistrationScope() {
        RegionCity otherCity = new RegionCity("Busan", "Busan", null, "26");
        ReflectionTestUtils.setField(otherCity, "id", 11L);
        RegionDistrict otherDistrict = new RegionDistrict(otherCity, "Haeundae-gu", "Haeundae-gu", null, "26350");
        ReflectionTestUtils.setField(otherDistrict, "id", 21L);
        RegionDong otherDong = new RegionDong(otherCity, otherDistrict, "U-dong", "U-dong", null, "2635065100");
        ReflectionTestUtils.setField(otherDong, "id", 32L);

        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(otherDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("DISTRICT");
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(USER_ID, createRequest()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getRegistrationPolicy_returnsUsageAndPolicyValues() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("CITY");
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(2L);

        var response = placeService.getRegistrationPolicy(USER_ID);

        assertThat(response.canRegister()).isTrue();
        assertThat(response.registrationScope()).isEqualTo("CITY");
        assertThat(response.registrationLimit()).isEqualTo(5);
        assertThat(response.currentUsage()).isEqualTo(2L);
    }

    @Test
    void getMyRegisteredPlaces_returnsNonDeletedPlacesCreatedByUser() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        PlaceStats stats = new PlaceStats(place);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findTop50ByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(place));
        when(placeStatsRepository.findById(100L)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.getMyRegisteredPlaces(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(100L);
        assertThat(response.getFirst().name()).isEqualTo("Test Place");
    }

    @Test
    void getNearbyPlaces_returnsPlacesInsideRadiusWithDistance() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        PlaceStats stats = new PlaceStats(place);
        when(placeRepository.findByDeletedAtIsNullAndExposureStatus(PlaceExposureStatus.VISIBLE))
                .thenReturn(List.of(place));
        when(placeStatsRepository.findById(100L)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.getNearbyPlaces(37.5500000, 126.9100000, 100);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().distanceMeter()).isEqualTo(0);
    }

    @Test
    void searchPlaces_returnsNameMatches() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        PlaceStats stats = new PlaceStats(place);
        when(placeSearchDocumentService.searchVisiblePlaces("Place")).thenReturn(List.of(place));
        when(placeStatsRepository.findById(100L)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.searchPlaces("Place");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().name()).isEqualTo("Test Place");
        verify(placeSearchDocumentService).searchVisiblePlaces("Place");
    }

    @Test
    void updatePlace_allowsOwnerToUpdateFieldsAndReplaceImages() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        PlaceImage updatedImage = new PlaceImage(
                place,
                "https://image.example.com/new-place.jpg",
                0,
                true,
                user
        );
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of(updatedImage));

        var response = placeService.updatePlace(
                USER_ID,
                100L,
                updateRequest("  Updated Place  ", null, List.of("https://image.example.com/new-place.jpg"))
        );

        assertThat(response.placeId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("Updated Place");
        assertThat(response.imageUrls()).containsExactly("https://image.example.com/new-place.jpg");
        verify(placeImageRepository).deleteByPlaceId(100L);
        verify(placeImageRepository).save(any(PlaceImage.class));
        verify(placeSearchDocumentService).syncPlace(place);
    }

    @Test
    void updatePlace_rejectsFeatureTextLongerThanPolicyLimit() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(policyService.getRequiredInteger("place", "feature_text_max_length")).thenReturn(5);

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateTextRequest(null, null, "123456")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(place.getFeatureText()).isEqualTo("Fast service and rich taste");
    }

    @Test
    void updatePlace_rejectsNonOwnerUser() {
        User otherUser = new User("other", "other@example.com");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        otherUser.verifyPhone("01099998888");
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(2L, 100L, updateRequest("Updated Place", null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updatePlace_rejectsUnverifiedNormalOwner() {
        User unverifiedOwner = new User("owner", "owner@example.com");
        ReflectionTestUtils.setField(unverifiedOwner, "id", USER_ID);
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        ReflectionTestUtils.setField(place, "createdBy", unverifiedOwner);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(unverifiedOwner));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateRequest("Updated Place", null, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_VERIFICATION_REQUIRED));
        verify(userSanctionRepository, never()).existsBlockingSanction(any(), any(), anyCollection(), any());
    }

    @Test
    void updatePlace_rejectsNormalOwnerWithActiveBlockingSanction() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(userSanctionRepository.existsBlockingSanction(eq(USER_ID), any(), anyCollection(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateRequest("Updated Place", null, null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_SANCTION_ACTIVE));
    }

    @Test
    void updatePlace_logsAdminUpdate() {
        User admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", 10L);
        admin.promoteTo(UserRole.ADMIN);
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.updatePlace(10L, 100L, updateRequest("Admin Updated Place", null, null));

        assertThat(response.name()).isEqualTo("Admin Updated Place");
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
        verify(userSanctionRepository, never()).existsBlockingSanction(any(), any(), anyCollection(), any());
    }

    @Test
    void updatePlace_rejectsPartialCoordinateUpdate() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                coordinateOnlyUpdateRequest(BigDecimal.valueOf(37.5510000), null)
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void deletePlace_marksPlaceDeletedAndClearsStarLevel() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        place.updateCurrentStarLevel(2);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        var response = placeService.deletePlace(USER_ID, 100L);

        assertThat(response.deleted()).isTrue();
        assertThat(place.isDeleted()).isTrue();
        assertThat(place.getCurrentStarLevel()).isZero();
        verify(placeSearchDocumentService).deletePlace(100L);
    }

    @Test
    void deletePlace_logsAdminDelete() {
        User admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", 10L);
        admin.promoteTo(UserRole.SUPER_ADMIN);
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        var response = placeService.deletePlace(10L, 100L);

        assertThat(response.deleted()).isTrue();
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    private PlaceCreateRequest createRequest() {
        return createRequestWithText(
                "Test Menu",
                "Local favorite worth revisiting",
                "Fast service and rich taste"
        );
    }

    private PlaceCreateRequest createRequestWithText(
            String recommendedMenu,
            String shortRecommendation,
            String featureText
    ) {
        return new PlaceCreateRequest(
                "Test Place",
                "KOREAN",
                31L,
                "1 Test Road",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                recommendedMenu,
                shortRecommendation,
                featureText,
                false,
                List.of("https://image.example.com/place.jpg")
        );
    }

    private Place createPlaceEntity(RegionDong dong) {
        return new Place(
                user,
                dong,
                "Test Place",
                "KOREAN",
                "1 Test Road",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Test Menu",
                "Local favorite worth revisiting",
                "Fast service and rich taste",
                false
        );
    }

    private PlaceUpdateRequest updateRequest(String name, Long dongId, List<String> imageUrls) {
        return new PlaceUpdateRequest(
                name,
                null,
                dongId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                imageUrls
        );
    }

    private PlaceUpdateRequest coordinateOnlyUpdateRequest(BigDecimal latitude, BigDecimal longitude) {
        return new PlaceUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                latitude,
                longitude,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private PlaceUpdateRequest updateTextRequest(
            String recommendedMenu,
            String shortRecommendation,
            String featureText
    ) {
        return new PlaceUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                recommendedMenu,
                shortRecommendation,
                featureText,
                null,
                null
        );
    }

    private void stubPlaceTextPolicies(
            int recommendedMenuMaxLength,
            int shortRecommendationMaxLength,
            int featureTextMaxLength
    ) {
        when(policyService.getRequiredInteger("place", "recommended_menu_max_length"))
                .thenReturn(recommendedMenuMaxLength);
        when(policyService.getRequiredInteger("place", "short_recommendation_max_length"))
                .thenReturn(shortRecommendationMaxLength);
        when(policyService.getRequiredInteger("place", "feature_text_max_length"))
                .thenReturn(featureTextMaxLength);
    }
}
