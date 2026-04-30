package com.honeytong.ranking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.ranking.dto.AdminRankingRecalculateRequest;
import com.honeytong.ranking.dto.AdminRankingRecalculateResponse;
import com.honeytong.ranking.dto.AdminSeasonCreateRequest;
import com.honeytong.ranking.dto.AdminSeasonResponse;
import com.honeytong.ranking.dto.AdminSeasonUpdateRequest;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRankingService {

    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String RECOMMEND_WEIGHT_KEY = "recommend_weight";
    private static final String VISIT_WEIGHT_KEY = "visit_weight";
    private static final String COMMENT_WEIGHT_KEY = "comment_weight";
    private static final String SEASON_TARGET_TYPE = "SEASON";
    private static final String RANKING_TARGET_TYPE = "RANKING";
    private static final String SEASON_CREATE_ACTION = "SEASON_CREATE";
    private static final String SEASON_UPDATE_ACTION = "SEASON_UPDATE";
    private static final String RANKING_RECALCULATE_ACTION = "RANKING_RECALCULATE";

    private final SeasonRepository seasonRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final PlaceSeasonScoreRepository placeSeasonScoreRepository;
    private final PolicyService policyService;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;

    public AdminRankingService(
            SeasonRepository seasonRepository,
            PlaceStatsRepository placeStatsRepository,
            PlaceSeasonScoreRepository placeSeasonScoreRepository,
            PolicyService policyService,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            ObjectMapper objectMapper
    ) {
        this.seasonRepository = seasonRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.placeSeasonScoreRepository = placeSeasonScoreRepository;
        this.policyService = policyService;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
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
        Season season = resolveSeason(request == null ? null : request.seasonCode());
        RankingWeights weights = loadWeights();

        placeSeasonScoreRepository.deleteBySeasonId(season.getId());
        List<PlaceStats> stats = placeStatsRepository.findAll().stream()
                .filter(this::isVisiblePlaceStats)
                .toList();

        Map<Long, Integer> maxStarByPlaceId = new HashMap<>();
        List<PlaceSeasonScore> scores = new ArrayList<>();
        scores.addAll(buildRegionScores(
                season,
                stats,
                RankingRegionType.DONG,
                place -> place.getRegionDong().getId(),
                1,
                weights,
                maxStarByPlaceId
        ));
        scores.addAll(buildRegionScores(
                season,
                stats,
                RankingRegionType.DISTRICT,
                place -> place.getRegionDistrict().getId(),
                2,
                weights,
                maxStarByPlaceId
        ));
        scores.addAll(buildRegionScores(
                season,
                stats,
                RankingRegionType.CITY,
                place -> place.getRegionCity().getId(),
                3,
                weights,
                maxStarByPlaceId
        ));

        placeSeasonScoreRepository.saveAll(scores);
        stats.forEach(stat -> stat.getPlace().updateCurrentStarLevel(
                maxStarByPlaceId.getOrDefault(stat.getPlace().getId(), 0)
        ));

        saveAdminLog(
                admin,
                RANKING_RECALCULATE_ACTION,
                RANKING_TARGET_TYPE,
                season.getId(),
                null,
                serializeRankingResult(season, stats.size(), scores.size()),
                request == null ? null : request.memo()
        );
        return new AdminRankingRecalculateResponse(season.getSeasonCode(), stats.size(), scores.size());
    }

    private List<PlaceSeasonScore> buildRegionScores(
            Season season,
            List<PlaceStats> stats,
            RankingRegionType regionType,
            Function<Place, Long> regionIdExtractor,
            int winnerStarLevel,
            RankingWeights weights,
            Map<Long, Integer> maxStarByPlaceId
    ) {
        Map<Long, List<PlaceStats>> statsByRegion = stats.stream()
                .collect(Collectors.groupingBy(stat -> regionIdExtractor.apply(stat.getPlace())));
        List<PlaceSeasonScore> scores = new ArrayList<>();
        for (Map.Entry<Long, List<PlaceStats>> entry : statsByRegion.entrySet()) {
            List<PlaceStats> rankedStats = entry.getValue().stream()
                    .sorted(Comparator
                            .comparing((PlaceStats stat) -> calculateScore(stat, weights).totalScore())
                            .reversed()
                            .thenComparing(stat -> stat.getPlace().getName()))
                    .toList();
            for (int index = 0; index < rankedStats.size(); index++) {
                PlaceStats stat = rankedStats.get(index);
                int rank = index + 1;
                int starLevel = rank == 1 ? winnerStarLevel : 0;
                if (starLevel > 0) {
                    maxStarByPlaceId.merge(stat.getPlace().getId(), starLevel, Math::max);
                }
                ScoreComponents components = calculateScore(stat, weights);
                scores.add(new PlaceSeasonScore(
                        season,
                        stat.getPlace(),
                        regionType,
                        entry.getKey(),
                        components.recommendScore(),
                        components.visitScore(),
                        components.commentScore(),
                        components.recentBonusScore(),
                        components.diversityBonusScore(),
                        components.trustBonusScore(),
                        components.totalScore(),
                        rank,
                        starLevel
                ));
            }
        }
        return scores;
    }

    private ScoreComponents calculateScore(PlaceStats stats, RankingWeights weights) {
        BigDecimal recommendScore = BigDecimal.valueOf(stats.getRecommendCount()).multiply(weights.recommendWeight());
        BigDecimal visitScore = BigDecimal.valueOf(stats.getVisitCount()).multiply(weights.visitWeight());
        BigDecimal commentScore = BigDecimal.valueOf(stats.getCommentCount()).multiply(weights.commentWeight());
        BigDecimal recentBonusScore = stats.getRecentScore();
        BigDecimal diversityBonusScore = stats.getDiversityScore();
        BigDecimal trustBonusScore = stats.getTrustWeightedScore();
        BigDecimal totalScore = recommendScore
                .add(visitScore)
                .add(commentScore)
                .add(recentBonusScore)
                .add(diversityBonusScore)
                .add(trustBonusScore);
        return new ScoreComponents(
                recommendScore,
                visitScore,
                commentScore,
                recentBonusScore,
                diversityBonusScore,
                trustBonusScore,
                totalScore
        );
    }

    private RankingWeights loadWeights() {
        return new RankingWeights(
                policyService.getRequiredDecimal(RANKING_POLICY_GROUP, RECOMMEND_WEIGHT_KEY),
                policyService.getRequiredDecimal(RANKING_POLICY_GROUP, VISIT_WEIGHT_KEY),
                policyService.getRequiredDecimal(RANKING_POLICY_GROUP, COMMENT_WEIGHT_KEY)
        );
    }

    private Season resolveSeason(String seasonCode) {
        if (seasonCode == null || seasonCode.isBlank()) {
            return seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "진행 중인 랭킹 시즌이 없습니다."));
        }
        return seasonRepository.findBySeasonCode(seasonCode.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "시즌을 찾을 수 없습니다."));
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

    private boolean isVisiblePlaceStats(PlaceStats stats) {
        Place place = stats.getPlace();
        return !place.isDeleted() && place.getExposureStatus() == PlaceExposureStatus.VISIBLE;
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

    private String serializeRankingResult(Season season, int placeCount, int scoreCount) {
        return serialize(Map.of(
                "seasonId", season.getId(),
                "seasonCode", season.getSeasonCode(),
                "placeCount", placeCount,
                "scoreCount", scoreCount
        ));
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "관리자 작업 로그를 생성할 수 없습니다.");
        }
    }

    private record RankingWeights(
            BigDecimal recommendWeight,
            BigDecimal visitWeight,
            BigDecimal commentWeight
    ) {
    }

    private record ScoreComponents(
            BigDecimal recommendScore,
            BigDecimal visitScore,
            BigDecimal commentScore,
            BigDecimal recentBonusScore,
            BigDecimal diversityBonusScore,
            BigDecimal trustBonusScore,
            BigDecimal totalScore
    ) {
    }
}
