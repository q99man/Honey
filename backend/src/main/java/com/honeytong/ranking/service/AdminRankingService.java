package com.honeytong.ranking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.ranking.cache.RankingCache;
import com.honeytong.ranking.dto.AdminRankingHistoryFinalizeRequest;
import com.honeytong.ranking.dto.AdminRankingHistoryFinalizeResponse;
import com.honeytong.ranking.dto.AdminRankingPlaceExclusionRequest;
import com.honeytong.ranking.dto.AdminRankingPlaceExclusionResponse;
import com.honeytong.ranking.dto.AdminRankingRecalculateRequest;
import com.honeytong.ranking.dto.AdminRankingRecalculateResponse;
import com.honeytong.ranking.dto.AdminSeasonCreateRequest;
import com.honeytong.ranking.dto.AdminSeasonResponse;
import com.honeytong.ranking.dto.AdminSeasonUpdateRequest;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRankingService {

    private static final String SEASON_TARGET_TYPE = "SEASON";
    private static final String RANKING_TARGET_TYPE = "RANKING";
    private static final String PLACE_TARGET_TYPE = "PLACE";
    private static final String SEASON_CREATE_ACTION = "SEASON_CREATE";
    private static final String SEASON_UPDATE_ACTION = "SEASON_UPDATE";
    private static final String RANKING_RECALCULATE_ACTION = "RANKING_RECALCULATE";
    private static final String RANKING_HISTORY_FINALIZE_ACTION = "RANKING_HISTORY_FINALIZE";
    private static final String PLACE_RANKING_EXCLUSION_ACTION = "PLACE_RANKING_EXCLUSION_UPDATE";

    private final SeasonRepository seasonRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final RankingCache rankingCache;
    private final RankingRecalculationService rankingRecalculationService;
    private final RankingHistoryFinalizationService rankingHistoryFinalizationService;
    private final ObjectMapper objectMapper;

    public AdminRankingService(
            SeasonRepository seasonRepository,
            PlaceRepository placeRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            RankingCache rankingCache,
            RankingRecalculationService rankingRecalculationService,
            RankingHistoryFinalizationService rankingHistoryFinalizationService,
            ObjectMapper objectMapper
    ) {
        this.seasonRepository = seasonRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.rankingCache = rankingCache;
        this.rankingRecalculationService = rankingRecalculationService;
        this.rankingHistoryFinalizationService = rankingHistoryFinalizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminSeasonResponse> getSeasons(Long adminUserId) {
        ensureAdmin(adminUserId);
        return seasonRepository.findAllByOrderByStartAtDesc().stream()
                .map(AdminSeasonResponse::from)
                .toList();
    }

    @Transactional
    public AdminSeasonResponse createSeason(Long adminUserId, AdminSeasonCreateRequest request) {
        User admin = ensureAdmin(adminUserId);
        validatePeriod(request.startAt(), request.endAt());
        if (seasonRepository.existsBySeasonCode(request.seasonCode().trim())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 존재하는 시즌 코드입니다.");
        }
        ensureOnlyActiveSeason(null, request.status());

        Season season = seasonRepository.save(new Season(
                request.seasonCode().trim(),
                request.seasonName().trim(),
                request.seasonType(),
                request.startAt(),
                request.endAt(),
                request.status()
        ));
        saveAdminLog(
                admin,
                SEASON_CREATE_ACTION,
                SEASON_TARGET_TYPE,
                season.getId(),
                null,
                serializeSeason(season),
                request.memo()
        );
        return AdminSeasonResponse.from(season);
    }

    @Transactional
    public AdminSeasonResponse updateSeason(Long adminUserId, Long seasonId, AdminSeasonUpdateRequest request) {
        User admin = ensureAdmin(adminUserId);
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "시즌을 찾을 수 없습니다."));

        String beforeValue = serializeSeason(season);
        String seasonName = request.seasonName() == null || request.seasonName().isBlank()
                ? season.getSeasonName()
                : request.seasonName().trim();
        SeasonType seasonType = request.seasonType() == null ? season.getSeasonType() : request.seasonType();
        LocalDateTime startAt = request.startAt() == null ? season.getStartAt() : request.startAt();
        LocalDateTime endAt = request.endAt() == null ? season.getEndAt() : request.endAt();
        SeasonStatus status = request.status() == null ? season.getStatus() : request.status();
        validatePeriod(startAt, endAt);
        ensureOnlyActiveSeason(season.getId(), status);
        season.update(seasonName, seasonType, startAt, endAt, status);
        String afterValue = serializeSeason(season);

        saveAdminLog(
                admin,
                SEASON_UPDATE_ACTION,
                SEASON_TARGET_TYPE,
                season.getId(),
                beforeValue,
                afterValue,
                request.memo()
        );
        return AdminSeasonResponse.from(season);
    }

    @Transactional
    public AdminRankingRecalculateResponse recalculatePlaceRankings(
            Long adminUserId,
            AdminRankingRecalculateRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        RankingRecalculationResult result = rankingRecalculationService.recalculate(
                request == null ? null : request.seasonCode()
        );

        saveAdminLog(
                admin,
                RANKING_RECALCULATE_ACTION,
                RANKING_TARGET_TYPE,
                result.seasonId(),
                null,
                serializeRankingResult(result),
                request == null ? null : request.memo()
        );
        return new AdminRankingRecalculateResponse(result.seasonCode(), result.placeCount(), result.scoreCount());
    }

    @Transactional
    public AdminRankingHistoryFinalizeResponse finalizeRankingHistory(
            Long adminUserId,
            Long seasonId,
            AdminRankingHistoryFinalizeRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        RankingHistoryFinalizationResult result = rankingHistoryFinalizationService.finalizeSeasonHistory(seasonId);

        saveAdminLog(
                admin,
                RANKING_HISTORY_FINALIZE_ACTION,
                SEASON_TARGET_TYPE,
                result.seasonId(),
                null,
                serializeHistoryFinalizationResult(result),
                request == null ? null : normalize(request.memo())
        );
        return new AdminRankingHistoryFinalizeResponse(
                result.seasonId(),
                result.seasonCode(),
                result.historyCount()
        );
    }

    @Transactional
    public AdminRankingPlaceExclusionResponse changePlaceRankingExclusion(
            Long adminUserId,
            Long placeId,
            AdminRankingPlaceExclusionRequest request
    ) {
        User admin = ensureAdmin(adminUserId);
        Place place = placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Place not found."));
        if (place.isRankingExcluded() != request.excluded()) {
            String beforeValue = serializePlaceRankingExclusion(place);
            place.changeRankingExcluded(request.excluded());
            saveAdminLog(
                    admin,
                    PLACE_RANKING_EXCLUSION_ACTION,
                    PLACE_TARGET_TYPE,
                    place.getId(),
                    beforeValue,
                    serializePlaceRankingExclusion(place),
                    normalize(request.memo())
            );
            rankingCache.evictAllPlaceRankings();
        }
        return new AdminRankingPlaceExclusionResponse(
                place.getId(),
                place.getName(),
                place.isRankingExcluded(),
                place.getCurrentStarLevel()
        );
    }

    private void ensureOnlyActiveSeason(Long currentSeasonId, SeasonStatus status) {
        if (status != SeasonStatus.ACTIVE) {
            return;
        }
        seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE)
                .filter(activeSeason -> !activeSeason.getId().equals(currentSeasonId))
                .ifPresent(activeSeason -> {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 진행 중인 랭킹 시즌이 있습니다.");
                });
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "시즌 시작일은 종료일보다 앞서야 합니다.");
        }
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return user;
    }

    private void saveAdminLog(
            User admin,
            String actionType,
            String targetType,
            Long targetId,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                actionType,
                targetType,
                targetId,
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializeSeason(Season season) {
        return serialize(Map.of(
                "seasonId", season.getId(),
                "seasonCode", season.getSeasonCode(),
                "seasonName", season.getSeasonName(),
                "seasonType", season.getSeasonType().name(),
                "startAt", season.getStartAt().toString(),
                "endAt", season.getEndAt().toString(),
                "status", season.getStatus().name()
        ));
    }

    private String serializeRankingResult(RankingRecalculationResult result) {
        return serialize(Map.of(
                "seasonId", result.seasonId(),
                "seasonCode", result.seasonCode(),
                "placeCount", result.placeCount(),
                "scoreCount", result.scoreCount()
        ));
    }

    private String serializeHistoryFinalizationResult(RankingHistoryFinalizationResult result) {
        return serialize(Map.of(
                "seasonId", result.seasonId(),
                "seasonCode", result.seasonCode(),
                "historyCount", result.historyCount()
        ));
    }

    private String serializePlaceRankingExclusion(Place place) {
        return serialize(Map.of(
                "placeId", place.getId(),
                "rankingExcluded", place.isRankingExcluded(),
                "currentStarLevel", place.getCurrentStarLevel()
        ));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "관리자 작업 로그를 생성할 수 없습니다.");
        }
    }
}
