package com.honeytong.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.dto.PlaceCreateRequest;
import com.honeytong.place.dto.PlaceCreateResponse;
import com.honeytong.place.dto.PlaceDeleteResponse;
import com.honeytong.place.dto.PlaceDetailResponse;
import com.honeytong.place.dto.PlaceListItemResponse;
import com.honeytong.place.dto.PlaceRegistrationPolicyResponse;
import com.honeytong.place.dto.PlaceUpdateRequest;
import com.honeytong.place.dto.PlaceUpdateResponse;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceAudienceStats;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceImage;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceAudienceStatsRepository;
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
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserSanctionStatus;
import com.honeytong.mission.entity.MissionTargetType;
import com.honeytong.mission.service.MissionService;
import com.honeytong.user.entity.UserSanctionType;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserSanctionRepository;
import com.honeytong.user.service.UserActionLogService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

    private static final List<UserSanctionType> BLOCKING_SANCTION_TYPES = List.of(
            UserSanctionType.TEMPORARY_RESTRICTION,
            UserSanctionType.PERMANENT_RESTRICTION
    );
    private static final String PLACE_POLICY_GROUP = "place";
    private static final String REGISTRATION_LIMIT_KEY = "registration_limit";
    private static final String RECOMMENDED_MENU_MAX_LENGTH_KEY = "recommended_menu_max_length";
    private static final String SHORT_RECOMMENDATION_MAX_LENGTH_KEY = "short_recommendation_max_length";
    private static final String FEATURE_TEXT_MAX_LENGTH_KEY = "feature_text_max_length";
    private static final String IMAGE_URL_MAX_LENGTH_KEY = "image_url_max_length";
    private static final String ADDRESS_MAX_LENGTH_KEY = "address_max_length";
    private static final int RECOMMENDED_MENU_COLUMN_LIMIT = 255;
    private static final int SHORT_RECOMMENDATION_COLUMN_LIMIT = 255;
    private static final int FEATURE_TEXT_COLUMN_LIMIT = 500;
    private static final int IMAGE_URL_COLUMN_LIMIT = 255;
    private static final int ADDRESS_COLUMN_LIMIT = 255;
    private static final String REGION_POLICY_GROUP = "region";
    private static final String REGISTRATION_SCOPE_KEY = "registration_scope";
    private static final String PLACE_TARGET_TYPE = "PLACE";
    private static final String PLACE_UPDATE_ACTION = "PLACE_UPDATE";
    private static final String PLACE_DELETE_ACTION = "PLACE_DELETE";

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final PlaceAudienceStatsRepository placeAudienceStatsRepository;
    private final PlaceAudienceStatsService placeAudienceStatsService;
    private final PlaceSearchDocumentService placeSearchDocumentService;
    private final RegionDongRepository regionDongRepository;
    private final UserRegionRepository userRegionRepository;
    private final UserRepository userRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final PolicyService policyService;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;
    private final UserActionLogService userActionLogService;
    private final MissionService missionService;

    public PlaceService(
            PlaceRepository placeRepository,
            PlaceImageRepository placeImageRepository,
            PlaceStatsRepository placeStatsRepository,
            PlaceAudienceStatsRepository placeAudienceStatsRepository,
            PlaceAudienceStatsService placeAudienceStatsService,
            PlaceSearchDocumentService placeSearchDocumentService,
            RegionDongRepository regionDongRepository,
            UserRegionRepository userRegionRepository,
            UserRepository userRepository,
            UserSanctionRepository userSanctionRepository,
            PolicyService policyService,
            AdminActionLogRepository adminActionLogRepository,
            ObjectMapper objectMapper,
            UserActionLogService userActionLogService,
            MissionService missionService
    ) {
        this.placeRepository = placeRepository;
        this.placeImageRepository = placeImageRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.placeAudienceStatsRepository = placeAudienceStatsRepository;
        this.placeAudienceStatsService = placeAudienceStatsService;
        this.placeSearchDocumentService = placeSearchDocumentService;
        this.regionDongRepository = regionDongRepository;
        this.userRegionRepository = userRegionRepository;
        this.userRepository = userRepository;
        this.userSanctionRepository = userSanctionRepository;
        this.policyService = policyService;
        this.adminActionLogRepository = adminActionLogRepository;
        this.objectMapper = objectMapper;
        this.userActionLogService = userActionLogService;
        this.missionService = missionService;
    }

    @Transactional
    public PlaceCreateResponse createPlace(Long userId, PlaceCreateRequest request) {
        User user = getActiveUser(userId);
        UserRegion userRegion = getPrimaryRegion(userId);
        RegionDong placeDong = regionDongRepository.findById(request.dongId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "등록 지역을 찾을 수 없습니다."));

        validateRegistrationLimit(userId);
        validateRegistrationScope(userRegion, placeDong);

        String recommendedMenu = optionalTextWithinPolicy(
                request.recommendedMenu(),
                RECOMMENDED_MENU_MAX_LENGTH_KEY,
                RECOMMENDED_MENU_COLUMN_LIMIT,
                "추천 메뉴 허용 길이를 초과했습니다."
        );
        String shortRecommendation = requiredTextWithinPolicy(
                request.shortRecommendation(),
                SHORT_RECOMMENDATION_MAX_LENGTH_KEY,
                SHORT_RECOMMENDATION_COLUMN_LIMIT,
                "지역 추천 문구를 입력해 주세요.",
                "지역 추천 문구 허용 길이를 초과했습니다."
        );
        String featureText = optionalTextWithinPolicy(
                request.featureText(),
                FEATURE_TEXT_MAX_LENGTH_KEY,
                FEATURE_TEXT_COLUMN_LIMIT,
                "특징 문구 허용 길이를 초과했습니다."
        );

        validateImageUrls(request.imageUrls());

        Place place = placeRepository.save(new Place(
                user,
                placeDong,
                request.name(),
                request.categoryCode(),
                optionalTextWithinPolicy(
                        request.addressRoad(),
                        ADDRESS_MAX_LENGTH_KEY,
                        ADDRESS_COLUMN_LIMIT,
                        "장소 주소 길이가 정책 허용치를 초과했습니다."
                ),
                optionalTextWithinPolicy(
                        request.addressJibun(),
                        ADDRESS_MAX_LENGTH_KEY,
                        ADDRESS_COLUMN_LIMIT,
                        "장소 주소 길이가 정책 허용치를 초과했습니다."
                ),
                request.latitude(),
                request.longitude(),
                request.priceRangeCode(),
                recommendedMenu,
                shortRecommendation,
                featureText,
                request.franchise()
        ));
        saveImages(place, request.imageUrls(), user);
        placeStatsRepository.save(new PlaceStats(place));
        placeAudienceStatsRepository.save(new PlaceAudienceStats(place));
        placeSearchDocumentService.syncPlace(place);
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_PLACE_CREATE,
                UserActionLogService.TARGET_PLACE,
                place.getId(),
                Map.of(
                        "categoryCode", place.getCategoryCode(),
                        "dongId", place.getRegionDong().getId(),
                        "franchise", place.isFranchise()
                )
        );
        missionService.trackProgress(userId, MissionTargetType.PLACE_REGISTER);

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
        return toDetailResponse(place, stats, currentImageUrls(placeId));
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
        return placeSearchDocumentService
                .searchVisiblePlaces(keyword.trim())
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

    @Transactional(readOnly = true)
    public List<PlaceListItemResponse> getMyRegisteredPlaces(Long userId) {
        getActiveUser(userId);
        return placeRepository.findTop50ByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(place -> toListItemResponse(place, getStats(place.getId()), null))
                .toList();
    }

    @Transactional
    public PlaceUpdateResponse updatePlace(Long userId, Long placeId, PlaceUpdateRequest request) {
        User actor = getActiveUser(userId);
        Place place = placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        ensureOwnerOrAdmin(actor, place);

        boolean adminActor = isAdmin(actor);
        if (!adminActor) {
            requirePhoneVerified(actor);
            requireNoActiveSanction(actor.getId());
        }

        String beforeValue = adminActor ? serializePlaceMutationState(place, currentImageUrls(place.getId())) : null;
        RegionDong nextDong = resolveDongForUpdate(actor, place, request.dongId(), adminActor);
        if (request.shortRecommendation() != null) {
            validatePolicyTextLength(
                    requiredTextOrCurrent(
                            request.shortRecommendation(),
                            place.getShortRecommendation(),
                            "지역 추천 문구를 입력해 주세요."
                    ),
                    SHORT_RECOMMENDATION_MAX_LENGTH_KEY,
                    SHORT_RECOMMENDATION_COLUMN_LIMIT,
                    "지역 추천 문구 허용 길이를 초과했습니다."
            );
        }
        place.updateDetails(
                nextDong,
                requiredTextOrCurrent(request.name(), place.getName(), "장소 이름을 입력해 주세요."),
                requiredTextOrCurrent(request.categoryCode(), place.getCategoryCode(), "카테고리를 입력해 주세요."),
                optionalTextWithinPolicyOrCurrent(
                        request.addressRoad(),
                        place.getAddressRoad(),
                        ADDRESS_MAX_LENGTH_KEY,
                        ADDRESS_COLUMN_LIMIT,
                        "장소 주소 길이가 정책 허용치를 초과했습니다."
                ),
                optionalTextWithinPolicyOrCurrent(
                        request.addressJibun(),
                        place.getAddressJibun(),
                        ADDRESS_MAX_LENGTH_KEY,
                        ADDRESS_COLUMN_LIMIT,
                        "장소 주소 길이가 정책 허용치를 초과했습니다."
                ),
                coordinateOrCurrent(request.latitude(), request.longitude(), place.getLatitude(), true),
                coordinateOrCurrent(request.latitude(), request.longitude(), place.getLongitude(), false),
                optionalTextOrCurrent(request.priceRangeCode(), place.getPriceRangeCode()),
                optionalTextWithinPolicyOrCurrent(
                        request.recommendedMenu(),
                        place.getRecommendedMenu(),
                        RECOMMENDED_MENU_MAX_LENGTH_KEY,
                        RECOMMENDED_MENU_COLUMN_LIMIT,
                        "추천 메뉴 허용 길이를 초과했습니다."
                ),
                requiredTextOrCurrent(
                        request.shortRecommendation(),
                        place.getShortRecommendation(),
                        "짧은 추천 문구를 입력해 주세요."
                ),
                optionalTextWithinPolicyOrCurrent(
                        request.featureText(),
                        place.getFeatureText(),
                        FEATURE_TEXT_MAX_LENGTH_KEY,
                        FEATURE_TEXT_COLUMN_LIMIT,
                        "특징 문구 허용 길이를 초과했습니다."
                ),
                request.franchise() == null ? place.isFranchise() : request.franchise()
        );
        if (request.imageUrls() != null) {
            replaceImages(place, request.imageUrls(), actor);
        }
        placeSearchDocumentService.syncPlace(place);

        List<String> imageUrls = currentImageUrls(place.getId());
        if (adminActor) {
            String afterValue = serializePlaceMutationState(place, imageUrls);
            if (!beforeValue.equals(afterValue)) {
                saveActionLog(actor, place, PLACE_UPDATE_ACTION, beforeValue, afterValue, null);
            }
        }
        return toUpdateResponse(place, imageUrls);
    }

    @Transactional
    public PlaceDeleteResponse deletePlace(Long userId, Long placeId) {
        User actor = getActiveUser(userId);
        Place place = placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        ensureOwnerOrAdmin(actor, place);

        boolean adminActor = isAdmin(actor);
        String beforeValue = adminActor ? serializePlaceDeletionState(place, false) : null;
        place.delete();
        placeSearchDocumentService.deletePlace(place.getId());
        if (adminActor) {
            saveActionLog(
                    actor,
                    place,
                    PLACE_DELETE_ACTION,
                    beforeValue,
                    serializePlaceDeletionState(place, true),
                    null
            );
        }
        return new PlaceDeleteResponse(place.getId(), true);
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
            throw new ApiException(ErrorCode.FORBIDDEN, "사용할 수 없는 계정입니다.");
        }
        return user;
    }

    private void ensureOwnerOrAdmin(User actor, Place place) {
        if (isAdmin(actor) || place.getCreatedBy().getId().equals(actor.getId())) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "장소 소유자 또는 관리자만 변경할 수 있습니다.");
    }

    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;
    }

    private void requirePhoneVerified(User user) {
        if (!user.isPhoneVerified()) {
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_REQUIRED);
        }
    }

    private void requireNoActiveSanction(Long userId) {
        boolean hasBlockingSanction = userSanctionRepository.existsBlockingSanction(
                userId,
                UserSanctionStatus.ACTIVE,
                BLOCKING_SANCTION_TYPES,
                LocalDateTime.now()
        );
        if (hasBlockingSanction) {
            throw new ApiException(ErrorCode.USER_SANCTION_ACTIVE);
        }
    }

    private UserRegion getPrimaryRegion(Long userId) {
        return userRegionRepository.findByUserIdAndPrimaryRegionTrueAndStatus(userId, UserRegionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_VERIFICATION_REQUIRED));
    }

    private RegionDong resolveDongForUpdate(User actor, Place place, Long dongId, boolean adminActor) {
        if (dongId == null) {
            return place.getRegionDong();
        }
        RegionDong nextDong = regionDongRepository.findById(dongId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "지역을 찾을 수 없습니다."));
        if (!adminActor) {
            validateRegistrationScope(getPrimaryRegion(actor.getId()), nextDong);
        }
        return nextDong;
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
            String normalizedImageUrl = optionalTextWithinPolicy(
                    imageUrl,
                    IMAGE_URL_MAX_LENGTH_KEY,
                    IMAGE_URL_COLUMN_LIMIT,
                    "장소 이미지 URL 길이가 정책 허용치를 초과했습니다."
            );
            if (normalizedImageUrl != null) {
                placeImageRepository.save(new PlaceImage(place, normalizedImageUrl, index, index == 0, user));
            }
        }
    }

    private void replaceImages(Place place, List<String> imageUrls, User user) {
        validateImageUrls(imageUrls);
        placeImageRepository.deleteByPlaceId(place.getId());
        saveImages(place, imageUrls, user);
    }

    private void validateImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            optionalTextWithinPolicy(
                    imageUrl,
                    IMAGE_URL_MAX_LENGTH_KEY,
                    IMAGE_URL_COLUMN_LIMIT,
                    "장소 이미지 URL 길이가 정책 허용치를 초과했습니다."
            );
        }
    }

    private List<String> currentImageUrls(Long placeId) {
        return placeImageRepository.findByPlaceIdOrderBySortOrderAsc(placeId).stream()
                .map(PlaceImage::getImageUrl)
                .toList();
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소 통계를 찾을 수 없습니다."));
    }

    private PlaceDetailResponse toDetailResponse(Place place, PlaceStats stats, List<String> imageUrls) {
        List<String> audienceTags = placeAudienceStatsService.generateAudienceTags(place.getId());
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
                imageUrls,
                audienceTags
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
                place.getLatitude(),
                place.getLongitude(),
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

    private PlaceUpdateResponse toUpdateResponse(Place place, List<String> imageUrls) {
        return new PlaceUpdateResponse(
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                place.getRegionCity().getId(),
                place.getRegionDistrict().getId(),
                place.getRegionDong().getId(),
                place.getAddressRoad(),
                place.getAddressJibun(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceRangeCode(),
                place.getRecommendedMenu(),
                place.getShortRecommendation(),
                place.getFeatureText(),
                place.isFranchise(),
                imageUrls
        );
    }

    private String requiredTextOrCurrent(String value, String currentValue, String message) {
        if (value == null) {
            return currentValue;
        }
        if (value.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private String optionalTextOrCurrent(String value, String currentValue) {
        if (value == null) {
            return currentValue;
        }
        if (value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String optionalTextWithinPolicy(
            String value,
            String policyKey,
            int columnLimit,
            String tooLongMessage
    ) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        validatePolicyTextLength(normalized, policyKey, columnLimit, tooLongMessage);
        return normalized;
    }

    private String requiredTextWithinPolicy(
            String value,
            String policyKey,
            int columnLimit,
            String blankMessage,
            String tooLongMessage
    ) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, blankMessage);
        }
        validatePolicyTextLength(normalized, policyKey, columnLimit, tooLongMessage);
        return normalized;
    }

    private String optionalTextWithinPolicyOrCurrent(
            String value,
            String currentValue,
            String policyKey,
            int columnLimit,
            String tooLongMessage
    ) {
        if (value == null) {
            return currentValue;
        }
        return optionalTextWithinPolicy(value, policyKey, columnLimit, tooLongMessage);
    }

    private void validatePolicyTextLength(
            String value,
            String policyKey,
            int columnLimit,
            String tooLongMessage
    ) {
        int maxLength = getBoundedPlaceTextMaxLength(policyKey, columnLimit);
        if (value.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, tooLongMessage);
        }
    }

    private int getBoundedPlaceTextMaxLength(String policyKey, int columnLimit) {
        int maxLength = policyService.getRequiredInteger(PLACE_POLICY_GROUP, policyKey);
        if (maxLength <= 0 || maxLength > columnLimit) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "장소 텍스트 길이 정책은 1-" + columnLimit + " 사이여야 합니다."
            );
        }
        return maxLength;
    }

    private BigDecimal coordinateOrCurrent(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal currentValue,
            boolean latitudeRequested
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "위도와 경도는 함께 수정해야 합니다.");
        }
        if (latitude == null) {
            return currentValue;
        }
        return latitudeRequested ? latitude : longitude;
    }

    private void saveActionLog(
            User admin,
            Place place,
            String actionType,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                actionType,
                PLACE_TARGET_TYPE,
                place.getId(),
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializePlaceMutationState(Place place, List<String> imageUrls) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("placeId", place.getId());
        state.put("name", place.getName());
        state.put("categoryCode", place.getCategoryCode());
        state.put("cityId", place.getRegionCity().getId());
        state.put("districtId", place.getRegionDistrict().getId());
        state.put("dongId", place.getRegionDong().getId());
        state.put("addressRoad", place.getAddressRoad());
        state.put("addressJibun", place.getAddressJibun());
        state.put("latitude", place.getLatitude());
        state.put("longitude", place.getLongitude());
        state.put("priceRangeCode", place.getPriceRangeCode());
        state.put("recommendedMenu", place.getRecommendedMenu());
        state.put("shortRecommendation", place.getShortRecommendation());
        state.put("featureText", place.getFeatureText());
        state.put("franchise", place.isFranchise());
        state.put("imageUrls", imageUrls);
        return serialize(state);
    }

    private String serializePlaceDeletionState(Place place, boolean deleted) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("placeId", place.getId());
        state.put("deleted", deleted);
        state.put("starLevel", place.getCurrentStarLevel());
        return serialize(state);
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "관리자 작업 로그를 생성할 수 없습니다.");
        }
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
