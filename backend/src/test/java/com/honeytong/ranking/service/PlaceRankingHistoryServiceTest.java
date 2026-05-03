package com.honeytong.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.ranking.entity.PlaceRankingHistory;
import com.honeytong.ranking.entity.RankingRegionType;
import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import com.honeytong.ranking.entity.SeasonType;
import com.honeytong.ranking.repository.PlaceRankingHistoryRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceRankingHistoryServiceTest {

    private static final long PLACE_ID = 100L;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceRankingHistoryRepository placeRankingHistoryRepository;

    private PlaceRankingHistoryService placeRankingHistoryService;
    private Place place;
    private Season season;

    @BeforeEach
    void setUp() {
        placeRankingHistoryService = new PlaceRankingHistoryService(
                placeRepository,
                placeRankingHistoryRepository
        );

        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);

        User owner = new User("owner", "owner@example.com");
        ReflectionTestUtils.setField(owner, "id", 1L);
        place = new Place(
                owner,
                dong,
                "Honey Soup",
                "KOREAN",
                "Mapo road 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "Soup",
                "A reliable local soup place",
                "Rich soup and fast service.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);

        season = new Season(
                "2026-04",
                "April 2026",
                SeasonType.MONTHLY,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 30, 23, 59),
                SeasonStatus.CLOSED
        );
        ReflectionTestUtils.setField(season, "id", 5L);
    }

    @Test
    void getPlaceRankingHistory_returnsFinalizedHistoryRows() {
        PlaceRankingHistory history = history(RankingRegionType.DONG, 30L, 1, 1, "42.50");
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeRankingHistoryRepository.findAllByPlaceIdForPublicHistory(PLACE_ID)).thenReturn(List.of(history));

        var response = placeRankingHistoryService.getPlaceRankingHistory(PLACE_ID);

        assertThat(response.placeId()).isEqualTo(PLACE_ID);
        assertThat(response.name()).isEqualTo("Honey Soup");
        assertThat(response.items()).hasSize(1);
        var item = response.items().getFirst();
        assertThat(item.seasonId()).isEqualTo(5L);
        assertThat(item.seasonCode()).isEqualTo("2026-04");
        assertThat(item.seasonName()).isEqualTo("April 2026");
        assertThat(item.seasonType()).isEqualTo("MONTHLY");
        assertThat(item.regionType()).isEqualTo("dong");
        assertThat(item.regionId()).isEqualTo(30L);
        assertThat(item.rank()).isEqualTo(1);
        assertThat(item.starLevel()).isEqualTo(1);
        assertThat(item.totalScore()).isEqualByComparingTo("42.50");
    }

    @Test
    void getPlaceRankingHistory_returnsEmptyItemsWhenHistoryDoesNotExist() {
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));
        when(placeRankingHistoryRepository.findAllByPlaceIdForPublicHistory(PLACE_ID)).thenReturn(List.of());

        var response = placeRankingHistoryService.getPlaceRankingHistory(PLACE_ID);

        assertThat(response.placeId()).isEqualTo(PLACE_ID);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getPlaceRankingHistory_rejectsHiddenPlace() {
        place.changeExposureStatus(PlaceExposureStatus.HIDDEN);
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> placeRankingHistoryService.getPlaceRankingHistory(PLACE_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(placeRankingHistoryRepository, never()).findAllByPlaceIdForPublicHistory(PLACE_ID);
    }

    @Test
    void getPlaceRankingHistory_rejectsDeletedPlace() {
        when(placeRepository.findByIdAndDeletedAtIsNull(PLACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeRankingHistoryService.getPlaceRankingHistory(PLACE_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verify(placeRankingHistoryRepository, never()).findAllByPlaceIdForPublicHistory(PLACE_ID);
    }

    private PlaceRankingHistory history(
            RankingRegionType regionType,
            Long regionId,
            int rank,
            int starLevel,
            String totalScore
    ) {
        PlaceRankingHistory history = new PlaceRankingHistory(
                place,
                season,
                regionType,
                regionId,
                rank,
                starLevel,
                new BigDecimal(totalScore)
        );
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.of(2026, 5, 1, 12, 0));
        return history;
    }
}
