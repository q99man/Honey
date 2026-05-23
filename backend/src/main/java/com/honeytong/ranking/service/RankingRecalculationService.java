package com.honeytong.ranking.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.ranking.cache.RankingCache;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import java.math.BigDecimal;
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
public class RankingRecalculationService {

    private static final String RANKING_POLICY_GROUP = "ranking";
    private static final String RECOMMEND_WEIGHT_KEY = "recommend_weight";
    private static final String VISIT_WEIGHT_KEY = "visit_weight";
    private static final String COMMENT_WEIGHT_KEY = "comment_weight";

    private final SeasonRepository seasonRepository;
    private final PlaceStatsRepository placeStatsRepository;
    private final PlaceSeasonScoreRepository placeSeasonScoreRepository;
    private final PolicyService policyService;
    private final RankingCache rankingCache;
    private final com.honeytong.recommendation.repository.RecommendationRepository recommendationRepository;
    private final com.honeytong.visit.repository.VisitRepository visitRepository;
    private final com.honeytong.comment.repository.CommentRepository commentRepository;
    private final com.honeytong.place.repository.PlaceAudienceStatsRepository placeAudienceStatsRepository;

    public RankingRecalculationService(
            SeasonRepository seasonRepository,
            PlaceStatsRepository placeStatsRepository,
            PlaceSeasonScoreRepository placeSeasonScoreRepository,
            PolicyService policyService,
            RankingCache rankingCache,
            com.honeytong.recommendation.repository.RecommendationRepository recommendationRepository,
            com.honeytong.visit.repository.VisitRepository visitRepository,
            com.honeytong.comment.repository.CommentRepository commentRepository,
            com.honeytong.place.repository.PlaceAudienceStatsRepository placeAudienceStatsRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.placeStatsRepository = placeStatsRepository;
        this.placeSeasonScoreRepository = placeSeasonScoreRepository;
        this.policyService = policyService;
        this.rankingCache = rankingCache;
        this.recommendationRepository = recommendationRepository;
        this.visitRepository = visitRepository;
        this.commentRepository = commentRepository;
        this.placeAudienceStatsRepository = placeAudienceStatsRepository;
    }

    @Transactional
    public RankingRecalculationResult recalculate(String seasonCode) {
        Season season = resolveSeason(seasonCode);
        RankingWeights weights = loadWeights();

        placeSeasonScoreRepository.deleteBySeasonId(season.getId());
        List<PlaceStats> stats = placeStatsRepository.findAll().stream()
                .filter(this::isVisiblePlaceStats)
                .toList();

        // Load recency & diversity policies
        int recencyDays = policyService.getRequiredInteger(RANKING_POLICY_GROUP, "recency_days");
        BigDecimal recencyRecommendWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, "recency_recommend_weight");
        BigDecimal recencyVisitWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, "recency_visit_weight");
        BigDecimal recencyCommentWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, "recency_comment_weight");
        BigDecimal diversityWeight = policyService.getRequiredDecimal(RANKING_POLICY_GROUP, "diversity_weight");

        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(recencyDays);

        for (PlaceStats stat : stats) {
            Long placeId = stat.getPlaceId();

            // Calculate Recency Score
            long recCount = recommendationRepository.countByPlaceIdAndStatusAndCreatedAtGreaterThanEqual(
                    placeId, com.honeytong.recommendation.entity.RecommendationStatus.ACTIVE, since);
            long visCount = visitRepository.countByPlaceIdAndValidTrueAndCreatedAtGreaterThanEqual(placeId, since);
            long comCount = commentRepository.countByPlaceIdAndStatusAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                    placeId, com.honeytong.comment.entity.CommentStatus.VISIBLE, since);

            BigDecimal recentScore = BigDecimal.valueOf(recCount).multiply(recencyRecommendWeight)
                    .add(BigDecimal.valueOf(visCount).multiply(recencyVisitWeight))
                    .add(BigDecimal.valueOf(comCount).multiply(recencyCommentWeight));

            // Calculate Diversity Score
            BigDecimal diversityScore = calculateDiversityScore(placeId, diversityWeight);

            stat.updateBonuses(recentScore, diversityScore);
            placeStatsRepository.save(stat);
        }

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
        rankingCache.evictAllPlaceRankings();
        return new RankingRecalculationResult(season.getId(), season.getSeasonCode(), stats.size(), scores.size());
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
        BigDecimal manualAdjustmentScore = stats.getManualAdjustmentScore();
        BigDecimal totalScore = recommendScore
                .add(visitScore)
                .add(commentScore)
                .add(recentBonusScore)
                .add(diversityBonusScore)
                .add(trustBonusScore)
                .add(manualAdjustmentScore)
                .max(BigDecimal.ZERO);
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

    private boolean isVisiblePlaceStats(PlaceStats stats) {
        Place place = stats.getPlace();
        return !place.isDeleted()
                && place.getExposureStatus() == PlaceExposureStatus.VISIBLE
                && !place.isRankingExcluded();
    }

    private BigDecimal calculateDiversityScore(Long placeId, BigDecimal diversityWeight) {
        return placeAudienceStatsRepository.findById(placeId)
                .map(stats -> {
                    // Age groups: 10s, 20s, 30s, 40s, 50s+
                    double[] ageCounts = {
                            stats.getAge10Count(),
                            stats.getAge20Count(),
                            stats.getAge30Count(),
                            stats.getAge40Count(),
                            stats.getAge50PlusCount()
                    };
                    double totalAge = 0;
                    for (double c : ageCounts) {
                        totalAge += c;
                    }
                    double ageEntropy = 0;
                    if (totalAge > 0) {
                        for (double c : ageCounts) {
                            if (c > 0) {
                                double p = c / totalAge;
                                ageEntropy -= p * Math.log(p);
                            }
                        }
                    }

                    // Gender groups: male, female, other
                    double[] genderCounts = {
                            stats.getMaleCount(),
                            stats.getFemaleCount(),
                            stats.getOtherGenderCount()
                    };
                    double totalGender = 0;
                    for (double c : genderCounts) {
                        totalGender += c;
                    }
                    double genderEntropy = 0;
                    if (totalGender > 0) {
                        for (double c : genderCounts) {
                            if (c > 0) {
                                double p = c / totalGender;
                                genderEntropy -= p * Math.log(p);
                            }
                        }
                    }

                    // Normalize entropies
                    double maxAgeEntropy = Math.log(5); // ln(5)
                    double maxGenderEntropy = Math.log(3); // ln(3)

                    double normAge = maxAgeEntropy > 0 ? ageEntropy / maxAgeEntropy : 0;
                    double normGender = maxGenderEntropy > 0 ? genderEntropy / maxGenderEntropy : 0;

                    double averageDiversity = (normAge + normGender) / 2.0;

                    return BigDecimal.valueOf(averageDiversity).multiply(diversityWeight);
                })
                .orElse(BigDecimal.ZERO);
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
