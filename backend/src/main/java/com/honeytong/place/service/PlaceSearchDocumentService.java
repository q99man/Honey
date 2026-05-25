package com.honeytong.place.service;

import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceExposureStatus;
import com.honeytong.place.entity.PlaceSearchDocument;
import com.honeytong.place.repository.PlaceSearchDocumentRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceSearchDocumentService {

    private final PlaceSearchDocumentRepository placeSearchDocumentRepository;
    private final EntityManager entityManager;

    public PlaceSearchDocumentService(
            PlaceSearchDocumentRepository placeSearchDocumentRepository,
            EntityManager entityManager
    ) {
        this.placeSearchDocumentRepository = placeSearchDocumentRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void syncPlace(Place place) {
        PlaceSearchDocument document = buildDocument(place);
        if (placeSearchDocumentRepository.existsById(place.getId())) {
            placeSearchDocumentRepository.save(document);
            return;
        }
        entityManager.persist(document);
    }

    @Transactional
    public void deletePlace(Long placeId) {
        placeSearchDocumentRepository.deleteById(placeId);
    }

    @Transactional(readOnly = true)
    public List<Place> searchVisiblePlaces(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return List.of();
        }

        PageRequest pageRequest = PageRequest.of(0, 50);
        String exposureStatusStr = PlaceExposureStatus.VISIBLE.name();

        List<PlaceSearchDocument> documents;
        if (normalized.length() < 2) {
            documents = placeSearchDocumentRepository.searchVisibleLike(normalized, exposureStatusStr, pageRequest);
        } else {
            String ftsQuery = buildFtsQuery(normalized);
            documents = placeSearchDocumentRepository.searchVisibleFts(ftsQuery, exposureStatusStr, pageRequest);
        }

        return documents.stream()
                .map(PlaceSearchDocument::getPlace)
                .toList();
    }

    private String buildFtsQuery(String keyword) {
        String[] tokens = keyword.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank()) {
                sb.append("+").append(token).append("* ");
            }
        }
        return sb.toString().trim();
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
