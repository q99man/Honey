package com.honeytong.place.service;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceSearchDocument;
import com.honeytong.place.repository.PlaceSearchDocumentRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceSearchDocumentService {

    private final PlaceSearchDocumentRepository placeSearchDocumentRepository;

    public PlaceSearchDocumentService(PlaceSearchDocumentRepository placeSearchDocumentRepository) {
        this.placeSearchDocumentRepository = placeSearchDocumentRepository;
    }

    @Transactional
    public void syncPlace(Place place) {
        placeSearchDocumentRepository.save(buildDocument(place));
    }

    @Transactional
    public void deletePlace(Long placeId) {
        placeSearchDocumentRepository.deleteById(placeId);
    }

    @Transactional(readOnly = true)
    public List<Place> searchVisiblePlaces(String keyword) {
        return placeSearchDocumentRepository.searchVisible(
                        normalizeKeyword(keyword),
                        PlaceExposureStatus.VISIBLE,
                        PageRequest.of(0, 50)
                )
                .stream()
                .map(PlaceSearchDocument::getPlace)
                .toList();
    }

    PlaceSearchDocument buildDocument(Place place) {
        return new PlaceSearchDocument(place, buildSearchText(place));
    }

    private String buildSearchText(Place place) {
        return Stream.of(
                        place.getName(),
                        place.getCategoryCode(),
                        place.getRecommendedMenu(),
                        place.getShortRecommendation(),
                        place.getFeatureText(),
                        place.getAddressRoad(),
                        place.getAddressJibun(),
                        place.getRegionCity().getNameKo(),
                        place.getRegionCity().getNameEn(),
                        place.getRegionDistrict().getNameKo(),
                        place.getRegionDistrict().getNameEn(),
                        place.getRegionDong().getNameKo(),
                        place.getRegionDong().getNameEn()
                )
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeKeyword)
                .distinct()
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}
