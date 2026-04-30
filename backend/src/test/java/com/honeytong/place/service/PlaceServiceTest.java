package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.place.dto.PlaceCreateRequest;
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
class PlaceServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceImageRepository placeImageRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private RegionDongRepository regionDongRepository;

    @Mock
    private UserRegionRepository userRegionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyService policyService;

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
                regionDongRepository,
                userRegionRepository,
                userRepository,
                policyService
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        userDong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(userDong, "id", 30L);
        targetDong = new RegionDong(city, district, "합정동", "Hapjeong-dong", null, "1144068000");
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
    }

    @Test
    void createPlace_rejectsPlaceOutsideRegistrationScope() {
        RegionCity otherCity = new RegionCity("부산광역시", "Busan", null, "26");
        ReflectionTestUtils.setField(otherCity, "id", 11L);
        RegionDistrict otherDistrict = new RegionDistrict(otherCity, "해운대구", "Haeundae-gu", null, "26350");
        ReflectionTestUtils.setField(otherDistrict, "id", 21L);
        RegionDong otherDong = new RegionDong(otherCity, otherDistrict, "우동", "U-dong", null, "2635065100");
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
        when(placeRepository
                .findTop50ByNameContainingIgnoreCaseAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                        "국밥",
                        PlaceExposureStatus.VISIBLE
                )).thenReturn(List.of(place));
        when(placeStatsRepository.findById(100L)).thenReturn(Optional.of(stats));
        when(placeImageRepository.findByPlaceIdOrderBySortOrderAsc(100L)).thenReturn(List.of());

        var response = placeService.searchPlaces("국밥");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().name()).isEqualTo("합정 국밥");
    }

    private PlaceCreateRequest createRequest() {
        return new PlaceCreateRequest(
                "합정 국밥",
                "KOREAN",
                31L,
                "서울 마포구 양화로 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "국밥",
                "동네에서 다시 찾고 싶은 국밥집",
                "맑은 국물과 빠른 회전이 좋습니다.",
                false,
                List.of("https://image.example.com/place.jpg")
        );
    }

    private Place createPlaceEntity(RegionDong dong) {
        return new Place(
                user,
                dong,
                "합정 국밥",
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
    }
}
