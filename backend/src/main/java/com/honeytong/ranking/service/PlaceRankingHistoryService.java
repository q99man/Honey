package com.honeytong.ranking.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.ranking.dto.PlaceRankingHistoryItemResponse;
import com.honeytong.ranking.dto.PlaceRankingHistoryResponse;
import com.honeytong.ranking.entity.PlaceRankingHistory;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.repository.PlaceRankingHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceRankingHistoryService {

    private static final String PLACE_NOT_FOUND_MESSAGE = "장소를 찾을 수 없습니다.";

    private final PlaceRepository placeRepository;
    private final PlaceRankingHistoryRepository placeRankingHistoryRepository;

    public PlaceRankingHistoryService(
            PlaceRepository placeRepository,
            PlaceRankingHistoryRepository placeRankingHistoryRepository
    ) {
        this.placeRepository = placeRepository;
        this.placeRankingHistoryRepository = placeRankingHistoryRepository;
    }

    @Transactional(readOnly = true)
    public PlaceRankingHistoryResponse getPlaceRankingHistory(Long placeId) {
        Place place = placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, PLACE_NOT_FOUND_MESSAGE));
        if (place.getExposureStatus() != PlaceExposureStatus.VISIBLE) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, PLACE_NOT_FOUND_MESSAGE);
        }

        List<PlaceRankingHistoryItemResponse> items = placeRankingHistoryRepository
                .findAllByPlaceIdForPublicHistory(placeId)
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new PlaceRankingHistoryResponse(place.getId(), place.getName(), items);
    }

    private PlaceRankingHistoryItemResponse toItemResponse(PlaceRankingHistory history) {
        Season season = history.getSeason();
        return new PlaceRankingHistoryItemResponse(
                season.getId(),
                season.getSeasonCode(),
                season.getSeasonName(),
                season.getSeasonType().name(),
                season.getStartAt(),
                season.getEndAt(),
                history.getRegionType().toApiValue(),
                history.getRegionRefId(),
                history.getRankNo(),
                history.getStarLevel(),
                history.getTotalScore(),
                history.getCreatedAt()
        );
    }
}
