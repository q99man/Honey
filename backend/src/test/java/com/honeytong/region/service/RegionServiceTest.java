package com.honeytong.region.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.dto.RegionChangeRequest;
import com.honeytong.region.dto.RegionVerifyRequest;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.repository.UserRegionRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
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
class RegionServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private RegionCityRepository regionCityRepository;

    @Mock
    private RegionDistrictRepository regionDistrictRepository;

    @Mock
    private RegionDongRepository regionDongRepository;

    @Mock
    private UserRegionRepository userRegionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private PolicyService policyService;

    private RegionService regionService;
    private User user;
    private RegionCity city;
    private RegionDistrict district;
    private RegionDong dong;
    private RegionDong nextDong;

    @BeforeEach
    void setUp() {
        city = new RegionCity("서울특별시", "Seoul", null, "1100000000");
        ReflectionTestUtils.setField(city, "id", 1L);
        district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "1144000000");
        ReflectionTestUtils.setField(district, "id", 2L);
        dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 3L);
        nextDong = new RegionDong(city, district, "합정동", "Hapjeong-dong", null, "1144068000");
        ReflectionTestUtils.setField(nextDong, "id", 4L);

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        regionService = new RegionService(
                regionCityRepository,
                regionDistrictRepository,
                regionDongRepository,
                userRegionRepository,
                userRepository,
                userTrustRepository,
                (latitude, longitude) -> new ResolvedRegion(dong),
                policyService
        );
    }

    @Test
    void getCities_returnsCities() {
        when(regionCityRepository.findAllByOrderByNameKoAsc()).thenReturn(List.of(city));

        var cities = regionService.getCities();

        assertThat(cities).hasSize(1);
        assertThat(cities.getFirst().nameKo()).isEqualTo("서울특별시");
    }

    @Test
    void getDistricts_validatesCityAndReturnsDistricts() {
        when(regionCityRepository.existsById(1L)).thenReturn(true);
        when(regionDistrictRepository.findByCityIdOrderByNameKoAsc(1L)).thenReturn(List.of(district));

        var districts = regionService.getDistricts(1L);

        assertThat(districts).hasSize(1);
        assertThat(districts.getFirst().cityId()).isEqualTo(1L);
    }

    @Test
    void verifyRegion_savesPrimaryRegionAndMarksTrust() {
        UserTrust userTrust = new UserTrust(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(any(), any()))
                .thenReturn(Optional.empty());
        when(userRegionRepository.save(any(UserRegion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));

        var response = regionService.verifyRegion(
                USER_ID,
                new RegionVerifyRequest(BigDecimal.valueOf(37.555), BigDecimal.valueOf(126.923))
        );

        assertThat(response.verified()).isTrue();
        assertThat(response.cityId()).isEqualTo(1L);
        assertThat(response.districtId()).isEqualTo(2L);
        assertThat(response.dongId()).isEqualTo(3L);
        assertThat(userTrust.isRegionVerified()).isTrue();
    }

    @Test
    void getRegionChangePolicy_allowsChangeWhenCooldownIsZero() {
        UserRegion currentRegion = new UserRegion(user, dong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(any(), any()))
                .thenReturn(Optional.of(currentRegion));
        when(policyService.getRequiredInteger("region", "change_cooldown_day")).thenReturn(0);

        var policy = regionService.getRegionChangePolicy(USER_ID);

        assertThat(policy.changeAllowed()).isTrue();
        assertThat(policy.cooldownDays()).isZero();
        assertThat(policy.nextAvailableAt()).isNull();
    }

    @Test
    void changeMyRegion_deactivatesCurrentRegionAndSavesNewRegion() {
        UserRegion currentRegion = new UserRegion(user, dong);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(any(), any()))
                .thenReturn(Optional.of(currentRegion));
        when(regionDongRepository.findById(4L)).thenReturn(Optional.of(nextDong));
        when(policyService.getRequiredInteger("region", "change_cooldown_day")).thenReturn(0);
        when(userRegionRepository.save(any(UserRegion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = regionService.changeMyRegion(USER_ID, new RegionChangeRequest(4L));

        assertThat(currentRegion.isPrimaryRegion()).isFalse();
        assertThat(response.dongId()).isEqualTo(4L);
        assertThat(response.dongName()).isEqualTo("합정동");
    }
}
