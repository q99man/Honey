package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceSearchDocument;
import com.honeytong.place.repository.PlaceSearchDocumentRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceSearchDocumentServiceTest {

    private static final long PLACE_ID = 100L;

    @Mock
    private PlaceSearchDocumentRepository placeSearchDocumentRepository;

    private PlaceSearchDocumentService placeSearchDocumentService;
    private Place place;

    @BeforeEach
    void setUp() {
        placeSearchDocumentService = new PlaceSearchDocumentService(placeSearchDocumentRepository);

        User owner = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(owner, "id", 1L);
        RegionCity city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);
        place = new Place(
                owner,
                dong,
                "꿀통분식",
                "KOREAN",
                "서울 마포구 어울마당로 1",
                "서울 마포구 서교동 1",
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "김치찌개",
                "다시 방문하고 싶은 동네 밥집",
                "점심 혼밥에 좋은 따뜻한 식당",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
    }

    @Test
    void syncPlace_savesNormalizedSearchTextFromPlaceAndRegionFields() {
        when(placeSearchDocumentRepository.save(any(PlaceSearchDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        placeSearchDocumentService.syncPlace(place);

        verify(placeSearchDocumentRepository).save(any(PlaceSearchDocument.class));
        var document = placeSearchDocumentService.buildDocument(place);
        assertThat(document.getSearchText())
                .contains("꿀통분식")
                .contains("김치찌개")
                .contains("마포구")
                .contains("seogyo-dong");
    }

    @Test
    void searchVisiblePlaces_usesNormalizedKeywordAndReturnsPlaces() {
        PlaceSearchDocument document = placeSearchDocumentService.buildDocument(place);
        when(placeSearchDocumentRepository.searchVisible("김치", PlaceExposureStatus.VISIBLE, PageRequest.of(0, 50)))
                .thenReturn(List.of(document));

        List<Place> result = placeSearchDocumentService.searchVisiblePlaces("  김치  ");

        assertThat(result).containsExactly(place);
        verify(placeSearchDocumentRepository).searchVisible(
                eq("김치"),
                eq(PlaceExposureStatus.VISIBLE),
                eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void deletePlace_removesDocumentByPlaceId() {
        placeSearchDocumentService.deletePlace(PLACE_ID);

        verify(placeSearchDocumentRepository).deleteById(PLACE_ID);
    }
}
