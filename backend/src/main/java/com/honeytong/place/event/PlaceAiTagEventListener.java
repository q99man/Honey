package com.honeytong.place.event;

import com.honeytong.comment.event.CommentCreatedEvent;
import com.honeytong.place.entity.Place;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.service.PlaceAiTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PlaceAiTagEventListener {

    private static final Logger log = LoggerFactory.getLogger(PlaceAiTagEventListener.class);

    private final PlaceRepository placeRepository;
    private final PlaceAiTagService placeAiTagService;

    public PlaceAiTagEventListener(
            PlaceRepository placeRepository,
            PlaceAiTagService placeAiTagService
    ) {
        this.placeRepository = placeRepository;
        this.placeAiTagService = placeAiTagService;
    }

    @Async("taskExecutor")
    @EventListener
    public void handlePlaceCreated(PlaceCreatedEvent event) {
        log.info("Received PlaceCreatedEvent for placeId: {}", event.placeId());
        placeRepository.findById(event.placeId()).ifPresentOrElse(
                place -> {
                    try {
                        placeAiTagService.generateAndSaveTags(place);
                    } catch (Exception e) {
                        log.error("Failed to generate AI tags for new place: {}", e.getMessage(), e);
                    }
                },
                () -> log.warn("Place not found for id: {}", event.placeId())
        );
    }

    @Async("taskExecutor")
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("Received CommentCreatedEvent for placeId: {}", event.placeId());
        placeRepository.findById(event.placeId()).ifPresentOrElse(
                place -> {
                    try {
                        placeAiTagService.generateAndSaveTags(place);
                    } catch (Exception e) {
                        log.error("Failed to regenerate AI tags after comment creation: {}", e.getMessage(), e);
                    }
                },
                () -> log.warn("Place not found for id: {}", event.placeId())
        );
    }
}
