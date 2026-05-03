package com.honeytong.ranking.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.ranking.entity.PlaceRankingHistory;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.repository.PlaceRankingHistoryRepository;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingHistoryFinalizationService {

    private final SeasonRepository seasonRepository;
    private final PlaceSeasonScoreRepository placeSeasonScoreRepository;
    private final PlaceRankingHistoryRepository placeRankingHistoryRepository;

    public RankingHistoryFinalizationService(
            SeasonRepository seasonRepository,
            PlaceSeasonScoreRepository placeSeasonScoreRepository,
            PlaceRankingHistoryRepository placeRankingHistoryRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.placeSeasonScoreRepository = placeSeasonScoreRepository;
        this.placeRankingHistoryRepository = placeRankingHistoryRepository;
    }

    @Transactional
    public RankingHistoryFinalizationResult finalizeSeasonHistory(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "시즌을 찾을 수 없습니다."));
        List<PlaceSeasonScore> scores = placeSeasonScoreRepository
                .findAllBySeasonIdOrderByRegionTypeAscRegionRefIdAscRankNoAsc(season.getId());
        if (scores.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "확정할 랭킹 점수가 없습니다.");
        }

        placeRankingHistoryRepository.deleteBySeasonId(season.getId());
        List<PlaceRankingHistory> histories = scores.stream()
                .map(this::toHistory)
                .toList();
        placeRankingHistoryRepository.saveAll(histories);
        return new RankingHistoryFinalizationResult(season.getId(), season.getSeasonCode(), histories.size());
    }

    private PlaceRankingHistory toHistory(PlaceSeasonScore score) {
        return new PlaceRankingHistory(
                score.getPlace(),
                score.getSeason(),
                score.getRegionType(),
                score.getRegionRefId(),
                requireRankNo(score),
                score.getStarLevel(),
                score.getTotalScore()
        );
    }

    private int requireRankNo(PlaceSeasonScore score) {
        if (score.getRankNo() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "순위가 없는 랭킹 점수는 히스토리로 확정할 수 없습니다.");
        }
        return score.getRankNo();
    }
}
