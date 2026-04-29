package com.honeytong.region.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.dto.MyRegionResponse;
import com.honeytong.region.dto.RegionChangePolicyResponse;
import com.honeytong.region.dto.RegionChangeRequest;
import com.honeytong.region.dto.RegionCityResponse;
import com.honeytong.region.dto.RegionDistrictResponse;
import com.honeytong.region.dto.RegionDongResponse;
import com.honeytong.region.dto.RegionVerificationResponse;
import com.honeytong.region.dto.RegionVerifyRequest;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.entity.UserRegionStatus;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.repository.UserRegionRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {

    private static final String REGION_POLICY_GROUP = "region";
    private static final String CHANGE_COOLDOWN_POLICY_KEY = "change_cooldown_day";

    private final RegionCityRepository regionCityRepository;
    private final RegionDistrictRepository regionDistrictRepository;
    private final RegionDongRepository regionDongRepository;
    private final UserRegionRepository userRegionRepository;
    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final RegionCoordinateResolver regionCoordinateResolver;
    private final PolicyService policyService;

    public RegionService(
            RegionCityRepository regionCityRepository,
            RegionDistrictRepository regionDistrictRepository,
            RegionDongRepository regionDongRepository,
            UserRegionRepository userRegionRepository,
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            RegionCoordinateResolver regionCoordinateResolver,
            PolicyService policyService
    ) {
        this.regionCityRepository = regionCityRepository;
        this.regionDistrictRepository = regionDistrictRepository;
        this.regionDongRepository = regionDongRepository;
        this.userRegionRepository = userRegionRepository;
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.regionCoordinateResolver = regionCoordinateResolver;
        this.policyService = policyService;
    }

    @Transactional(readOnly = true)
    public List<RegionCityResponse> getCities() {
        return regionCityRepository.findAllByOrderByNameKoAsc().stream()
                .map(this::toCityResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegionDistrictResponse> getDistricts(Long cityId) {
        ensureCityExists(cityId);
        return regionDistrictRepository.findByCityIdOrderByNameKoAsc(cityId).stream()
                .map(this::toDistrictResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegionDongResponse> getDongs(Long districtId) {
        ensureDistrictExists(districtId);
        return regionDongRepository.findByDistrictIdOrderByNameKoAsc(districtId).stream()
                .map(this::toDongResponse)
                .toList();
    }

    @Transactional
    public RegionVerificationResponse verifyRegion(Long userId, RegionVerifyRequest request) {
        User user = getActiveUser(userId);
        RegionDong dong = regionCoordinateResolver.resolve(request.latitude(), request.longitude()).dong();

        userRegionRepository
                .findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .ifPresent(UserRegion::deactivate);
        UserRegion userRegion = userRegionRepository.save(new UserRegion(user, dong));
        userTrustRepository.findById(userId).ifPresent(UserTrust::markRegionVerified);

        return toVerificationResponse(userRegion);
    }

    @Transactional(readOnly = true)
    public MyRegionResponse getMyRegion(Long userId) {
        getActiveUser(userId);
        return userRegionRepository
                .findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .map(this::toMyRegionResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "인증된 기본 지역을 찾을 수 없습니다."));
    }

    @Transactional
    public MyRegionResponse changeMyRegion(Long userId, RegionChangeRequest request) {
        User user = getActiveUser(userId);
        UserRegion currentRegion = getCurrentRegion(userId);
        RegionDong nextDong = regionDongRepository.findById(request.dongId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "변경할 지역을 찾을 수 없습니다."));

        if (currentRegion.getDong().getId().equals(nextDong.getId())) {
            throw new ApiException(ErrorCode.INVALID_REGION_CHANGE, "현재 기본 지역과 같은 지역입니다.");
        }

        RegionChangePolicyResponse policy = getRegionChangePolicy(currentRegion);
        if (!policy.changeAllowed()) {
            throw new ApiException(ErrorCode.INVALID_REGION_CHANGE, "아직 지역을 변경할 수 없습니다.");
        }

        currentRegion.deactivate();
        UserRegion changedRegion = userRegionRepository.save(new UserRegion(user, nextDong));
        return toMyRegionResponse(changedRegion);
    }

    @Transactional(readOnly = true)
    public RegionChangePolicyResponse getRegionChangePolicy(Long userId) {
        getActiveUser(userId);
        return userRegionRepository
                .findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .map(this::getRegionChangePolicy)
                .orElseGet(() -> new RegionChangePolicyResponse(true, getRegionChangeCooldownDays(), null));
    }

    private RegionChangePolicyResponse getRegionChangePolicy(UserRegion currentRegion) {
        int cooldownDays = getRegionChangeCooldownDays();
        LocalDateTime nextAvailableAt = currentRegion.getChangedAt().plusDays(cooldownDays);
        boolean changeAllowed = !LocalDateTime.now().isBefore(nextAvailableAt);
        return new RegionChangePolicyResponse(changeAllowed, cooldownDays, changeAllowed ? null : nextAvailableAt);
    }

    private int getRegionChangeCooldownDays() {
        int cooldownDays = policyService.getRequiredInteger(REGION_POLICY_GROUP, CHANGE_COOLDOWN_POLICY_KEY);
        if (cooldownDays < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "지역 변경 쿨다운 정책은 0 이상이어야 합니다.");
        }
        return cooldownDays;
    }

    private UserRegion getCurrentRegion(Long userId) {
        return userRegionRepository
                .findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_VERIFICATION_REQUIRED));
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private void ensureCityExists(Long cityId) {
        if (!regionCityRepository.existsById(cityId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "시/도를 찾을 수 없습니다.");
        }
    }

    private void ensureDistrictExists(Long districtId) {
        if (!regionDistrictRepository.existsById(districtId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "시/군/구를 찾을 수 없습니다.");
        }
    }

    private RegionCityResponse toCityResponse(RegionCity city) {
        return new RegionCityResponse(
                city.getId(),
                city.getNameKo(),
                city.getNameEn(),
                city.getNameJa(),
                city.getCode()
        );
    }

    private RegionDistrictResponse toDistrictResponse(RegionDistrict district) {
        return new RegionDistrictResponse(
                district.getId(),
                district.getCity().getId(),
                district.getNameKo(),
                district.getNameEn(),
                district.getNameJa(),
                district.getCode()
        );
    }

    private RegionDongResponse toDongResponse(RegionDong dong) {
        return new RegionDongResponse(
                dong.getId(),
                dong.getCity().getId(),
                dong.getDistrict().getId(),
                dong.getNameKo(),
                dong.getNameEn(),
                dong.getNameJa(),
                dong.getCode()
        );
    }

    private RegionVerificationResponse toVerificationResponse(UserRegion userRegion) {
        return new RegionVerificationResponse(
                userRegion.getCity().getId(),
                userRegion.getDistrict().getId(),
                userRegion.getDong().getId(),
                userRegion.getCity().getNameKo(),
                userRegion.getDistrict().getNameKo(),
                userRegion.getDong().getNameKo(),
                true
        );
    }

    private MyRegionResponse toMyRegionResponse(UserRegion userRegion) {
        return new MyRegionResponse(
                userRegion.getCity().getId(),
                userRegion.getDistrict().getId(),
                userRegion.getDong().getId(),
                userRegion.getCity().getNameKo(),
                userRegion.getDistrict().getNameKo(),
                userRegion.getDong().getNameKo(),
                true
        );
    }
}
