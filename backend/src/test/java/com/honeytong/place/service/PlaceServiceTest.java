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
import com.honeytong.place.entity.PlaceAudienceStats;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceImage;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceAudienceStatsRepository;
import com.honeytong.place.repository.PlaceImageRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.place.service.PlaceAddressCoordinateResolver.ResolvedPlaceCoordinate;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.entity.UserRegionStatus;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.repository.UserRegionRepository;
import com.honeytong.region.service.RegionCoordinateResolver;
import com.honeytong.region.service.ResolvedRegion;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserSanctionRepository;
import com.honeytong.user.service.UserActionLogService;
import com.honeytong.fraud.service.FraudDetectionService;
import org.springframework.context.ApplicationEventPublisher;
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
    private PlaceAudienceStatsRepository placeAudienceStatsRepository;

    @Mock
    private PlaceAudienceStatsService placeAudienceStatsService;

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

    @Mock
    private com.honeytong.mission.service.MissionService missionService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PlaceAiTagService placeAiTagService;

    @Mock
    private PlaceAddressCoordinateResolver placeAddressCoordinateResolver;

    @Mock
    private RegionCoordinateResolver regionCoordinateResolver;

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
                placeAudienceStatsRepository,
                placeAudienceStatsService,
                placeSearchDocumentService,
                regionDongRepository,
                userRegionRepository,
                userRepository,
                userSanctionRepository,
                policyService,
                adminActionLogRepository,
                new ObjectMapper(),
                userActionLogService,
                missionService,
                fraudDetectionService,
                eventPublisher,
                placeAiTagService,
                placeAddressCoordinateResolver,
                regionCoordinateResolver
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

        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredInteger("place", "image_url_max_length"))
                .thenReturn(255);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredInteger("place", "address_max_length"))
                .thenReturn(255);
        org.mockito.Mockito.lenient()
                .when(placeAddressCoordinateResolver.resolve(any()))
                .thenReturn(new ResolvedPlaceCoordinate(
                        BigDecimal.valueOf(37.5500000),
                        BigDecimal.valueOf(126.9100000)
                ));
        org.mockito.Mockito.lenient()
                .when(regionCoordinateResolver.resolve(any(), any()))
                .thenReturn(new ResolvedRegion(targetDong));
    }

    @Test
    void createPlace_savesPlaceImagesAndStatsWhenPolicyAllows() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            ReflectionTestUtils.setField(place, "id", 100L);
            return place;
        });

        var response = placeService.createPlace(USER_ID, createRequest(), "127.0.0.1");

        assertThat(response.placeId()).isEqualTo(100L);
        verify(placeImageRepository).save(any(PlaceImage.class));
        verify(placeStatsRepository).save(any(PlaceStats.class));
        verify(placeAudienceStatsRepository).save(any(PlaceAudienceStats.class));
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
    void createPlace_usesAddressResolvedCoordinatesWhenAddressIsPresent() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);
        when(placeAddressCoordinateResolver.resolve("서울특별시 마포구 와우산로 23길 9"))
                .thenReturn(new ResolvedPlaceCoordinate(
                        BigDecimal.valueOf(37.5564560),
                        BigDecimal.valueOf(126.9244560)
                ));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            ReflectionTestUtils.setField(place, "id", 100L);
            return place;
        });

        placeService.createPlace(
                USER_ID,
                createRequestWithAddress("서울특별시 마포구 와우산로 23길 9", null),
                "127.0.0.1"
        );

        org.mockito.ArgumentCaptor<Place> placeCaptor = org.mockito.ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(placeCaptor.capture());
        assertThat(placeCaptor.getValue().getLatitude()).isEqualByComparingTo("37.5564560");
        assertThat(placeCaptor.getValue().getLongitude()).isEqualByComparingTo("126.9244560");
    }

    @Test
    void createPlace_usesAddressResolvedRegionWhenAddressIsPresent() {
        RegionDong addressDong = new RegionDong(city, district, "Sangsu-dong", "Sangsu-dong", null, "1144065500");
        ReflectionTestUtils.setField(addressDong, "id", 32L);
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);
        when(placeAddressCoordinateResolver.resolve("서울특별시 마포구 와우산로 23길 9"))
                .thenReturn(new ResolvedPlaceCoordinate(
                        BigDecimal.valueOf(37.5564560),
                        BigDecimal.valueOf(126.9244560)
                ));
        when(regionCoordinateResolver.resolve(
                BigDecimal.valueOf(37.5564560),
                BigDecimal.valueOf(126.9244560)
        )).thenReturn(new ResolvedRegion(addressDong));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            ReflectionTestUtils.setField(place, "id", 100L);
            return place;
        });

        placeService.createPlace(
                USER_ID,
                createRequestWithAddress("서울특별시 마포구 와우산로 23길 9", null),
                "127.0.0.1"
        );

        org.mockito.ArgumentCaptor<Place> placeCaptor = org.mockito.ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(placeCaptor.capture());
        assertThat(placeCaptor.getValue().getRegionDong()).isEqualTo(addressDong);
    }

    @Test
    void createPlace_rejectsShortRecommendationLongerThanPolicyLimit() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        when(policyService.getRequiredInteger("place", "recommended_menu_max_length")).thenReturn(255);
        when(policyService.getRequiredInteger("place", "short_recommendation_max_length")).thenReturn(5);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(
                USER_ID,
                createRequestWithText("Test Menu", "123456", "Fast service"),
                "127.0.0.1"
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(placeRepository, never()).save(any());
        verify(placeStatsRepository, never()).save(any());
    }

    @Test
    void createPlace_rejectsImageUrlLongerThanPolicyLimit() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(policyService.getRequiredInteger("place", "image_url_max_length")).thenReturn(5);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(
                USER_ID,
                createRequestWithImages(List.of("123456")),
                "127.0.0.1"
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(placeRepository, never()).save(any(Place.class));
        verify(placeImageRepository, never()).save(any(PlaceImage.class));
        verify(placeStatsRepository, never()).save(any(PlaceStats.class));
    }

    @Test
    void createPlace_rejectsAddressLongerThanPolicyLimit() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(policyService.getRequiredInteger("place", "address_max_length")).thenReturn(5);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));
        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        stubPlaceTextPolicies(255, 255, 500);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(
                USER_ID,
                createRequestWithAddress("123456", null),
                "127.0.0.1"
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(placeRepository, never()).save(any(Place.class));
        verify(placeStatsRepository, never()).save(any(PlaceStats.class));
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
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("DISTRICT");
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> placeService.createPlace(USER_ID, createRequest(), "127.0.0.1"))
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
        when(placeRepository.findNearbyPlaces(eq("POINT(126.9100000 37.5500000)"), eq("VISIBLE"), eq(100)))
                .thenReturn(List.of(place));
        when(placeStatsRepository.findById(100L)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.getNearbyPlaces(37.5500000, 126.9100000, 100);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().distanceMeter()).isEqualTo(0);
        verify(placeRepository).findNearbyPlaces(eq("POINT(126.9100000 37.5500000)"), eq("VISIBLE"), eq(100));
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
    void updatePlace_usesAddressResolvedCoordinatesWhenAddressIsPresent() {
        RegionDong addressDong = new RegionDong(city, district, "Sangsu-dong", "Sangsu-dong", null, "1144065500");
        ReflectionTestUtils.setField(addressDong, "id", 32L);
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(placeAddressCoordinateResolver.resolve("Seoul Mapo Test Address 9"))
                .thenReturn(new ResolvedPlaceCoordinate(
                        BigDecimal.valueOf(37.5564560),
                        BigDecimal.valueOf(126.9244560)
                ));
        when(regionCoordinateResolver.resolve(
                BigDecimal.valueOf(37.5564560),
                BigDecimal.valueOf(126.9244560)
        )).thenReturn(new ResolvedRegion(addressDong));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(new UserRegion(user, userDong)));
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("DISTRICT");

        var response = placeService.updatePlace(
                USER_ID,
                100L,
                updateAddressRequest("Seoul Mapo Test Address 9", null)
        );

        assertThat(response.latitude()).isEqualByComparingTo("37.5564560");
        assertThat(response.longitude()).isEqualByComparingTo("126.9244560");
        assertThat(response.dongId()).isEqualTo(32L);
        assertThat(place.getLatitude()).isEqualByComparingTo("37.5564560");
        assertThat(place.getLongitude()).isEqualByComparingTo("126.9244560");
        assertThat(place.getRegionDong()).isEqualTo(addressDong);
        verify(placeSearchDocumentService).syncPlace(place);
    }

    @Test
    void updatePlace_rejectsAddressResolvedRegionOutsideRegistrationScope() {
        RegionCity otherCity = new RegionCity("Busan", "Busan", null, "26");
        ReflectionTestUtils.setField(otherCity, "id", 11L);
        RegionDistrict otherDistrict = new RegionDistrict(otherCity, "Haeundae-gu", "Haeundae-gu", null, "26350");
        ReflectionTestUtils.setField(otherDistrict, "id", 21L);
        RegionDong otherDong = new RegionDong(otherCity, otherDistrict, "U-dong", "U-dong", null, "2635065100");
        ReflectionTestUtils.setField(otherDong, "id", 33L);
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));
        when(placeAddressCoordinateResolver.resolve("Busan Haeundae Test Address"))
                .thenReturn(new ResolvedPlaceCoordinate(
                        BigDecimal.valueOf(35.1631130),
                        BigDecimal.valueOf(129.1635500)
                ));
        when(regionCoordinateResolver.resolve(
                BigDecimal.valueOf(35.1631130),
                BigDecimal.valueOf(129.1635500)
        )).thenReturn(new ResolvedRegion(otherDong));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(new UserRegion(user, userDong)));
        when(policyService.getRequiredString("region", "registration_scope")).thenReturn("DISTRICT");

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateAddressRequest("Busan Haeundae Test Address", null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));
        assertThat(place.getLatitude()).isEqualByComparingTo("37.5500000");
        assertThat(place.getLongitude()).isEqualByComparingTo("126.9100000");
        assertThat(place.getRegionDong()).isEqualTo(targetDong);
        verify(placeSearchDocumentService, never()).syncPlace(place);
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
    void updatePlace_rejectsImageUrlLongerThanPolicyLimit() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(policyService.getRequiredInteger("place", "image_url_max_length")).thenReturn(5);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateRequest(null, null, List.of("123456"))
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(placeImageRepository, never()).deleteByPlaceId(100L);
        verify(placeImageRepository, never()).save(any(PlaceImage.class));
    }

    @Test
    void updatePlace_rejectsAddressLongerThanPolicyLimit() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(policyService.getRequiredInteger("place", "address_max_length")).thenReturn(5);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                updateAddressRequest("123456", null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(place.getAddressRoad()).isEqualTo("1 Test Road");
        verify(placeSearchDocumentService, never()).syncPlace(place);
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

    private PlaceCreateRequest createRequestWithImages(List<String> imageUrls) {
        return new PlaceCreateRequest(
                "Test Place",
                "KOREAN",
                31L,
                "1 Test Road",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Test Menu",
                "Local favorite worth revisiting",
                "Fast service and rich taste",
                false,
                imageUrls
        );
    }

    private PlaceCreateRequest createRequestWithAddress(String addressRoad, String addressJibun) {
        return new PlaceCreateRequest(
                "Test Place",
                "KOREAN",
                31L,
                addressRoad,
                addressJibun,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Test Menu",
                "Local favorite worth revisiting",
                "Fast service and rich taste",
                false,
                List.of("https://image.example.com/place.jpg")
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

    private PlaceUpdateRequest updateAddressRequest(String addressRoad, String addressJibun) {
        return new PlaceUpdateRequest(
                null,
                null,
                null,
                addressRoad,
                addressJibun,
                null,
                null,
                null,
                null,
                null,
                null,
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

    @Test
    void getPlace_failsWhenNotFoundOrDeleted() {
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> placeService.getPlace(999L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        Place deletedPlace = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(deletedPlace, "id", 100L);
        deletedPlace.delete();
        when(placeRepository.findById(100L)).thenReturn(Optional.of(deletedPlace));
        assertThatThrownBy(() -> placeService.getPlace(100L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        Place invisiblePlace = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(invisiblePlace, "id", 101L);
        ReflectionTestUtils.setField(invisiblePlace, "exposureStatus", PlaceExposureStatus.HIDDEN);
        when(placeRepository.findById(101L)).thenReturn(Optional.of(invisiblePlace));
        assertThatThrownBy(() -> placeService.getPlace(101L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void getNearbyPlaces_failsWhenRadiusInvalid() {
        assertThatThrownBy(() -> placeService.getNearbyPlaces(37.5500000, 126.9100000, 0))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThatThrownBy(() -> placeService.getNearbyPlaces(37.5500000, 126.9100000, -10))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void searchPlaces_failsWhenKeywordBlank() {
        assertThatThrownBy(() -> placeService.searchPlaces(null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThatThrownBy(() -> placeService.searchPlaces("   "))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createPlace_failsWhenPolicyInvalid() {
        UserRegion userRegion = new UserRegion(user, userDong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(USER_ID, UserRegionStatus.ACTIVE))
                .thenReturn(Optional.of(userRegion));
        when(regionDongRepository.findById(31L)).thenReturn(Optional.of(targetDong));

        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(-1);
        assertThatThrownBy(() -> placeService.createPlace(USER_ID, createRequest(), "127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));

        when(policyService.getRequiredInteger("place", "registration_limit")).thenReturn(5);
        org.mockito.Mockito.lenient()
                .when(policyService.getRequiredString("region", "registration_scope"))
                .thenReturn("INVALID_SCOPE");
        assertThatThrownBy(() -> placeService.createPlace(USER_ID, createRequest(), "127.0.0.1"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));
    }

    @Test
    void updatePlace_failsWhenCoordinatePartial() {
        Place place = createPlaceEntity(targetDong);
        ReflectionTestUtils.setField(place, "id", 100L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                coordinateOnlyUpdateRequest(BigDecimal.valueOf(37.5510000), null)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThatThrownBy(() -> placeService.updatePlace(
                USER_ID,
                100L,
                coordinateOnlyUpdateRequest(null, BigDecimal.valueOf(126.9110000))
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
