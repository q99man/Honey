package com.honeytong.visit.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.visit.dto.MyVisitResponse;
import com.honeytong.visit.dto.PlaceVisitSummaryResponse;
import com.honeytong.visit.dto.VisitPolicyResponse;
import com.honeytong.visit.dto.VisitResponse;
import com.honeytong.visit.dto.VisitVerifyRequest;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.repository.VisitRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private static final String VISIT_POLICY_GROUP = "visit";
    private static final String RADIUS_METER_KEY = "radius_meter";
    private static final String COOLDOWN_HOUR_KEY = "cooldown_hour";
    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String VISIT_WEIGHT_KEY = "visit_weight";
    private static final String VALID_REASON = "VALID";

    private final VisitRepository visitRepository;
    private final PlaceRepository placeRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final UserRepository userRepository;
    private final PolicyService policyService;

    public VisitService(
            VisitRepository visitRepository,
            PlaceRepository placeRepository,
            PlaceStatsRepository placeStatsRepository,
            UserRepository userRepository,
            PolicyService policyService
    ) {
        this.visitRepository = visitRepository;
        this.placeRepository = placeRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.userRepository = userRepository;
        this.policyService = policyService;
    }

    @Transactional
    public VisitResponse verifyVisit(Long userId, Long placeId, VisitVerifyRequest request) {
        User user = getActiveUser(userId);
        Place place = getVisiblePlace(placeId);
        int radiusMeter = getRadiusMeter();
        validateCooldown(userId, placeId);

        int distanceMeter = calculateDistanceMeter(
                request.latitude().doubleValue(),
                request.longitude().doubleValue(),
                place.getLatitude().doubleValue(),
                place.getLongitude().doubleValue()
        );
        if (distanceMeter > radiusMeter) {
            throw new ApiException(ErrorCode.OUT_OF_VISIT_RADIUS);
        }

        visitRepository.save(new Visit(
                user,
                place,
                request.latitude(),
                request.longitude(),
                distanceMeter,
                normalizeImageUrl(request.imageUrl()),
                true,
                VALID_REASON
        ));

        PlaceStats stats = getStats(placeId);
        stats.addVisit(getVisitWeight());
        return new VisitResponse(true, distanceMeter, 0, stats.getVisitCount());
    }

    @Transactional(readOnly = true)
    public VisitPolicyResponse getVisitPolicy(Long userId, Long placeId) {
        getActiveUser(userId);
        getVisiblePlace(placeId);
        int radiusMeter = getRadiusMeter();
        LocalDateTime cooldownUntil = getCooldownUntil(userId, placeId);
        return new VisitPolicyResponse(cooldownUntil == null, radiusMeter, cooldownUntil);
    }

    @Transactional(readOnly = true)
    public List<MyVisitResponse> getMyVisits(Long userId) {
        getActiveUser(userId);
        return visitRepository.findByUserIdAndValidTrueOrderByCreatedAtDesc(userId).stream()
                .map(this::toMyVisitResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaceVisitSummaryResponse getPlaceVisitSummary(Long placeId) {
        getVisiblePlace(placeId);
        PlaceStats stats = getStats(placeId);
        LocalDateTime lastVisitedAt = visitRepository.findTopByPlaceIdAndValidTrueOrderByCreatedAtDesc(placeId)
                .map(Visit::getCreatedAt)
                .orElse(null);
        return new PlaceVisitSummaryResponse(placeId, stats.getVisitCount(), lastVisitedAt);
    }

    private void validateCooldown(Long userId, Long placeId) {
        if (getCooldownUntil(userId, placeId) != null) {
            throw new ApiException(ErrorCode.VISIT_COOLDOWN_ACTIVE);
        }
    }

    private LocalDateTime getCooldownUntil(Long userId, Long placeId) {
        int cooldownHour = getCooldownHour();
        if (cooldownHour <= 0) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return visitRepository.findTopByUserIdAndPlaceIdAndValidTrueOrderByCreatedAtDesc(userId, placeId)
                .map(visit -> visit.getCreatedAt().plusHours(cooldownHour))
                .filter(until -> until.isAfter(now))
                .orElse(null);
    }

    private int getRadiusMeter() {
        int radiusMeter = policyService.getRequiredInteger(VISIT_POLICY_GROUP, RADIUS_METER_KEY);
        if (radiusMeter <= 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "방문 인증 반경 정책은 1 이상이어야 합니다.");
        }
        return radiusMeter;
    }

    private int getCooldownHour() {
        int cooldownHour = policyService.getRequiredInteger(VISIT_POLICY_GROUP, COOLDOWN_HOUR_KEY);
        if (cooldownHour < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "방문 인증 쿨다운 정책은 0 이상이어야 합니다.");
        }
        return cooldownHour;
    }

    private BigDecimal getVisitWeight() {
        BigDecimal visitWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, VISIT_WEIGHT_KEY);
        if (visitWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "방문 점수 가중치 정책은 0 이상이어야 합니다.");
        }
        return visitWeight;
    }

    private Place getVisiblePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다."));
        if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        return place;
    }

    private PlaceStats getStats(Long placeId) {
        return placeStatsRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "장소 통계를 찾을 수 없습니다."));
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private MyVisitResponse toMyVisitResponse(Visit visit) {
        Place place = visit.getPlace();
        return new MyVisitResponse(
                visit.getId(),
                place.getId(),
                place.getName(),
                place.getCategoryCode(),
                place.getAddressRoad() == null ? place.getAddressJibun() : place.getAddressRoad(),
                visit.getDistanceMeter(),
                visit.getImageUrl(),
                visit.getCreatedAt()
        );
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
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
}
