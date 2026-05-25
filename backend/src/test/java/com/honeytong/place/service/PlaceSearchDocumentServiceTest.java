package com.honeytong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceSearchDocument;
import com.honeytong.place.repository.PlaceSearchDocumentRepository;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceSearchDocumentServiceTest {

    private static final long PLACE_ID = 100L;

    @Mock
    private PlaceSearchDocumentRepository placeSearchDocumentRepository;

    @Mock
    private EntityManager entityManager;

    private PlaceSearchDocumentService placeSearchDocumentService;
    private Place place;

    @BeforeEach
    void setUp() {
        placeSearchDocumentService = new PlaceSearchDocumentService(
                placeSearchDocumentRepository,
                entityManager
        );

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
                "서울 마포구 와우산로 1",
                "서울 마포구 서교동 1",
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "김치찌개",
                "다시 방문하고 싶은 동네 밥집",
                "점심 회전이 빠르고 국물이 진한 식당",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
    }

    @Test
    void syncPlace_persistsNewDocumentWhenDocumentDoesNotExist() {
        when(placeSearchDocumentRepository.existsById(PLACE_ID)).thenReturn(false);

        placeSearchDocumentService.syncPlace(place);

        ArgumentCaptor<PlaceSearchDocument> captor = ArgumentCaptor.forClass(PlaceSearchDocument.class);
        verify(entityManager).persist(captor.capture());
        verify(placeSearchDocumentRepository, never()).save(any(PlaceSearchDocument.class));
        assertThat(captor.getValue().getPlaceId()).isEqualTo(PLACE_ID);
        assertThat(captor.getValue().getSearchText()).contains("꿀통분식", "김치찌개", "마포구", "seogyo-dong");
    }

    @Test
    void syncPlace_savesExistingDocument() {
        when(placeSearchDocumentRepository.existsById(PLACE_ID)).thenReturn(true);
        when(placeSearchDocumentRepository.save(any(PlaceSearchDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        placeSearchDocumentService.syncPlace(place);

        verify(placeSearchDocumentRepository).save(any(PlaceSearchDocument.class));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void searchVisiblePlaces_usesFtsForTwoOrMoreCharacters() {
        PlaceSearchDocument document = placeSearchDocumentService.buildDocument(place);
        when(placeSearchDocumentRepository.searchVisibleFts("+김치*", "VISIBLE", PageRequest.of(0, 50)))
                .thenReturn(List.of(document));

        List<Place> result = placeSearchDocumentService.searchVisiblePlaces("  김치  ");

        assertThat(result).containsExactly(place);
        verify(placeSearchDocumentRepository).searchVisibleFts(
                eq("+김치*"),
                eq("VISIBLE"),
                eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void searchVisiblePlaces_fallsBackToLikeSearchForSingleCharacterKeyword() {
        PlaceSearchDocument document = placeSearchDocumentService.buildDocument(place);
        when(placeSearchDocumentRepository.searchVisibleLike("꿀", "VISIBLE", PageRequest.of(0, 50)))
                .thenReturn(List.of(document));

        List<Place> result = placeSearchDocumentService.searchVisiblePlaces("  꿀  ");

        assertThat(result).containsExactly(place);
        verify(placeSearchDocumentRepository).searchVisibleLike(
                eq("꿀"),
                eq("VISIBLE"),
                eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void searchVisiblePlaces_returnsEmptyListForEmptyKeyword() {
        List<Place> result = placeSearchDocumentService.searchVisiblePlaces("   ");
        assertThat(result).isEmpty();
    }

    @Test
    void deletePlace_removesDocumentByPlaceId() {
        placeSearchDocumentService.deletePlace(PLACE_ID);

        verify(placeSearchDocumentRepository).deleteById(PLACE_ID);
    }
}
