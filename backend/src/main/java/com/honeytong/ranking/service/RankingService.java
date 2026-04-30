package com.honeytong.ranking.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.ranking.dto.CurrentSeasonResponse;
import com.honeytong.ranking.dto.PlaceRankingItemResponse;
import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.entity.PlaceSeasonScore;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.repository.PlaceSeasonScoreRepository;
import com.honeytong.ranking.repository.SeasonRepository;
import com.honeytong.region.repository.RegionCityRepository;
import com.honeytong.region.repository.RegionDistrictRepository;
import com.honeytong.region.repository.RegionDongRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

    private final SeasonRepository seasonRepository;
    private final PlaceSeasonScoreRepository placeSeasonScoreRepository;
    private final RegionCityRepository regionCityRepository;
    private final RegionDistrictRepository regionDistrictRepository;
    private final RegionDongRepository regionDongRepository;

    public RankingService(
            SeasonRepository seasonRepository,
            PlaceSeasonScoreRepository placeSeasonScoreRepository,
            RegionCityRepository regionCityRepository,
            RegionDistrictRepository regionDistrictRepository,
            RegionDongRepository regionDongRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.placeSeasonScoreRepository = placeSeasonScoreRepository;
        this.regionCityRepository = regionCityRepository;
        this.regionDistrictRepository = regionDistrictRepository;
        this.regionDongRepository = regionDongRepository;
    }

    @Transactional(readOnly = true)
    public PlaceRankingResponse getPlaceRankings(String regionTypeValue, Long regionId, String seasonCode) {
        if (regionId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "랭킹 지역 ID를 입력해 주세요.");
        }
        RankingRegionType regionType = RankingRegionType.from(regionTypeValue);
        Season season = resolveSeason(seasonCode);
        String regionName = resolveRegionName(regionType, regionId);

        List<PlaceSeasonScore> scores = placeSeasonScoreRepository
                .findTop50BySeasonIdAndRegionTypeAndRegionRefIdOrderByRankNoAscTotalScoreDesc(
                        season.getId(),
                        regionType,
                        regionId
                );

        List<PlaceRankingItemResponse> items = new ArrayList<>();
        for (PlaceSeasonScore score : scores) {
            Place place = score.getPlace();
            if (place.isDeleted() || place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
                continue;
            }
            int rank = score.getRankNo() == null ? items.size() + 1 : score.getRankNo();
            items.add(new PlaceRankingItemResponse(
                    rank,
                    place.getId(),
                    place.getName(),
                    score.getStarLevel(),
                    score.getTotalScore(),
                    List.of()
            ));
        }

        return new PlaceRankingResponse(
                season.getSeasonCode(),
                regionType.toApiValue(),
                regionName,
                items
        );
    }

    @Transactional(readOnly = true)
    public CurrentSeasonResponse getCurrentSeason() {
        Season season = getActiveSeason();
        return new CurrentSeasonResponse(
                season.getSeasonCode(),
                season.getSeasonName(),
                season.getSeasonType().name(),
                season.getStartAt(),
                season.getEndAt(),
                season.getStatus().name()
        );
    }

    private Season resolveSeason(String seasonCode) {
        if (seasonCode == null || seasonCode.isBlank()) {
            return getActiveSeason();
        }
        return seasonRepository.findBySeasonCode(seasonCode.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "랭킹 시즌을 찾을 수 없습니다."));
    }

    private Season getActiveSeason() {
        return seasonRepository.findFirstByStatusOrderByStartAtDesc(SeasonStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "진행 중인 랭킹 시즌이 없습니다."));
    }

    private String resolveRegionName(RankingRegionType regionType, Long regionId) {
        return switch (regionType) {
            case DONG -> regionDongRepository.findById(regionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "랭킹 지역을 찾을 수 없습니다."))
                    .getNameKo();
            case DISTRICT -> regionDistrictRepository.findById(regionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "랭킹 지역을 찾을 수 없습니다."))
                    .getNameKo();
            case CITY -> regionCityRepository.findById(regionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "랭킹 지역을 찾을 수 없습니다."))
                    .getNameKo();
        };
    }
}
