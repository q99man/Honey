package com.honeytong.place.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.place.dto.PlaceCreateResponse;
import com.honeytong.place.dto.PlaceDetailResponse;
import com.honeytong.place.dto.PlaceListItemResponse;
import com.honeytong.place.dto.PlaceRegistrationPolicyResponse;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceImage;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceImageRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.entity.UserRegionStatus;
import com.honeytong.region.repository.RegionDongRepository;
import com.honeytong.region.repository.UserRegionRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

    private static final String PLACE_POLICY_GROUP = "place";
    private static final String REGISTRATION_LIMIT_KEY = "registration_limit";
    private static final String REGION_POLICY_GROUP = "region";
    private static final String REGISTRATION_SCOPE_KEY = "registration_scope";

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final RegionDongRepository regionDongRepository;
    private final UserRegionRepository userRegionRepository;
    private final UserRepository userRepository;
    private final PolicyService policyService;

    public PlaceService(
            PlaceRepository placeRepository,
            PlaceImageRepository placeImageRepository,
            PlaceStatsRepository placeStatsRepository,
            RegionDongRepository regionDongRepository,
            UserRegionRepository userRegionRepository,
            UserRepository userRepository,
            PolicyService policyService
    ) {
        this.placeRepository = placeRepository;
        this.placeImageRepository = placeImageRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.regionDongRepository = regionDongRepository;
        this.userRegionRepository = userRegionRepository;
        this.userRepository = userRepository;
        this.policyService = policyService;
    }

    @Transactional
    public PlaceCreateResponse createPlace(Long userId, PlaceCreateRequest request) {
        User user = getActiveUser(userId);
        UserRegion userRegion = getPrimaryRegion(userId);
        RegionDong placeDong = regionDongRepository.findById(request.dongId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "등록할 지역을 찾을 수 없습니다."));

        validateRegistrationLimit(userId);
        validateRegistrationScope(userRegion, placeDong);

        Place place = placeRepository.save(new Place(
                user,
                placeDong,
                request.name(),
                request.categoryCode(),
                request.addressRoad(),
                request.addressJibun(),
                request.latitude(),
                request.longitude(),
                request.priceRangeCode(),
                request.recommendedMenu(),
                request.shortRecommendation(),
                request.featureText(),
                request.franchise()
        ));
        saveImages(place, request.imageUrls(), user);
        placeStatsRepository.save(new PlaceStats(place));

        return new PlaceCreateResponse(place.getId(), place.getApprovalStatus());
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        PlaceStats stats = getStats(placeId);
        List<String> imageUrls = placeImageRepository.findByPlaceIdOrderBySortOrderAsc(placeId).stream()
                .map(PlaceImage::getImageUrl)
                .toList();
        return toDetailResponse(place, stats, imageUrls);
    }

    @Transactional(readOnly = true)
    public List<PlaceListItemResponse> getPlaces(Long cityId, Long districtId, Long dongId) {
        List<Place> places;
        if (dongId != null) {
            places = placeRepository.findTop50ByRegionDongIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                    dongId,
                    PlaceExposureStatus.VISIBLE
            );
        } else if (districtId != null) {
            places = placeRepository.findTop50ByRegionDistrictIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                    districtId,
                    PlaceExposureStatus.VISIBLE
            );
        } else if (cityId != null) {
            places = placeRepository.findTop50ByRegionCityIdAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                    cityId,
                    PlaceExposureStatus.VISIBLE
            );
        } else {
            places = placeRepository.findTop50ByDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                    PlaceExposureStatus.VISIBLE
            );
        }
        return places.stream()
                .map(place -> toListItemResponse(place, getStats(place.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceListItemResponse> getNearbyPlaces(double latitude, double longitude, int radiusMeter) {
        if (radiusMeter <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "검색 반경은 1 이상이어야 합니다.");
        }
        return placeRepository.findByDeletedAtIsNullAndExposureStatus(PlaceExposureStatus.VISIBLE).stream()
                .map(place -> new NearbyPlace(place, calculateDistanceMeter(
                        latitude,
                        longitude,
                        place.getLatitude().doubleValue(),
                        place.getLongitude().doubleValue()
                )))
                .filter(item -> item.distanceMeter() <= radiusMeter)
                .sorted(Comparator.comparingInt(NearbyPlace::distanceMeter))
                .limit(50)
                .map(item -> toListItemResponse(item.place(), getStats(item.place().getId()), item.distanceMeter()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceListItemResponse> searchPlaces(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "검색어를 입력해 주세요.");
        }
        return placeRepository
                .findTop50ByNameContainingIgnoreCaseAndDeletedAtIsNullAndExposureStatusOrderByCreatedAtDesc(
                        keyword.trim(),
                        PlaceExposureStatus.VISIBLE
                )
                .stream()
                .map(place -> toListItemResponse(place, getStats(place.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaceRegistrationPolicyResponse getRegistrationPolicy(Long userId) {
        getActiveUser(userId);
        int registrationLimit = getRegistrationLimit();
        long currentUsage = placeRepository.countByCreatedByIdAndDeletedAtIsNull(userId);
        return new PlaceRegistrationPolicyResponse(
                currentUsage < registrationLimit,
                getRegistrationScope().name(),
                registrationLimit,
                currentUsage
        );
    }

    private void validateRegistrationLimit(Long userId) {
        int registrationLimit = getRegistrationLimit();
        long currentUsage = placeRepository.countByCreatedByIdAndDeletedAtIsNull(userId);
        if (currentUsage >= registrationLimit) {
            throw new ApiException(ErrorCode.PLACE_REGISTRATION_LIMIT_EXCEEDED);
        }
    }

    private void validateRegistrationScope(UserRegion userRegion, RegionDong placeDong) {
        RegistrationScope scope = getRegistrationScope();
        boolean allowed = switch (scope) {
            case DONG -> userRegion.getDong().getId().equals(placeDong.getId());
            case DISTRICT -> userRegion.getDistrict().getId().equals(placeDong.getDistrict().getId());
            case CITY -> userRegion.getCity().getId().equals(placeDong.getCity().getId());
        };
        if (!allowed) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "장소 등록 허용 지역 범위를 벗어났습니다.");
        }
    }

    private int getRegistrationLimit() {
        int registrationLimit = policyService.getRequiredInteger(PLACE_POLICY_GROUP, REGISTRATION_LIMIT_KEY);
        if (registrationLimit < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "장소 등록 제한 정책은 0 이상이어야 합니다.");
        }
        return registrationLimit;
    }

    private RegistrationScope getRegistrationScope() {
        String value = policyService.getRequiredString(REGION_POLICY_GROUP, REGISTRATION_SCOPE_KEY);
        try {
            return RegistrationScope.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "장소 등록 지역 범위 정책이 올바르지 않습니다.");
        }
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private UserRegion getPrimaryRegion(Long userId) {
        return userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_VERIFICATION_REQUIRED));
    }

    private void saveImages(Place place, List<String> imageUrls, User user) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (int index = 0; index < imageUrls.size(); index++) {
            String imageUrl = imageUrls.get(index);
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            placeImageRepository.save(new PlaceImage(place, imageUrl.trim(), index, index == 0, user));
        }
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소 통계를 찾을 수 없습니다."));
    }

    private PlaceDetailResponse toDetailResponse(Place place, PlaceStats stats, List<String> imageUrls) {
        return new PlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                place.getRegionCity().getId(),
                place.getRegionDistrict().getId(),
                place.getRegionDong().getId(),
                place.getRegionCity().getNameKo(),
                place.getRegionDistrict().getNameKo(),
                place.getRegionDong().getNameKo(),
                place.getAddressRoad(),
                place.getAddressJibun(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceRangeCode(),
                place.getRecommendedMenu(),
                place.getShortRecommendation(),
                place.getFeatureText(),
                place.isFranchise(),
                place.getFranchiseReviewStatus(),
                place.getApprovalStatus(),
                place.getExposureStatus(),
                place.getCurrentStarLevel(),
                place.getCurrentFlowerGrade(),
                stats.getRecommendCount(),
                stats.getVisitCount(),
                stats.getCommentCount(),
                imageUrls
        );
    }

    private PlaceListItemResponse toListItemResponse(Place place, PlaceStats stats, Integer distanceMeter) {
        String representativeImageUrl = placeImageRepository.findByPlaceIdOrderBySortOrderAsc(place.getId()).stream()
                .findFirst()
                .map(PlaceImage::getImageUrl)
                .orElse(null);
        return new PlaceListItemResponse(
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                place.getRegionCity().getId(),
                place.getRegionDistrict().getId(),
                place.getRegionDong().getId(),
                place.getRegionDong().getNameKo(),
                place.getAddressRoad() == null ? place.getAddressJibun() : place.getAddressRoad(),
                place.getShortRecommendation(),
                place.getCurrentStarLevel(),
                place.getCurrentFlowerGrade(),
                stats.getRecommendCount(),
                stats.getVisitCount(),
                stats.getCommentCount(),
                distanceMeter,
                representativeImageUrl
        );
    }

    private int calculateDistanceMeter(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusMeter = 6_371_000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(earthRadiusMeter * c);
    }

    private enum RegistrationScope {
        DONG,
        DISTRICT,
        CITY
    }

    private record NearbyPlace(Place place, int distanceMeter) {
    }
}
