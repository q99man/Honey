package com.honeytong.place.service;

import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceAiTag;
import com.honeytong.place.repository.PlaceAiTagRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceAiTagService {

    private static final Logger log = LoggerFactory.getLogger(PlaceAiTagService.class);

    private final PlaceAiTagRepository placeAiTagRepository;
    private final CommentRepository commentRepository;
    private final AiTaggingService aiTaggingService;

    public PlaceAiTagService(
            PlaceAiTagRepository placeAiTagRepository,
            CommentRepository commentRepository,
            AiTaggingService aiTaggingService
    ) {
        this.placeAiTagRepository = placeAiTagRepository;
        this.commentRepository = commentRepository;
        this.aiTaggingService = aiTaggingService;
    }

    public List<String> getTagsByPlaceId(Long placeId) {
        return placeAiTagRepository.findByPlaceId(placeId).stream()
                .map(PlaceAiTag::getTagName)
                .collect(Collectors.toList());
    }

    @Transactional
    public void generateAndSaveTags(Place place) {
        log.info("Generating AI tags for place id: {}", place.getId());
        List<Comment> comments = commentRepository.findByPlaceIdAndStatusOrderByCreatedAtDesc(place.getId(), CommentStatus.VISIBLE);
        List<String> commentTexts = comments.stream()
                .map(Comment::getContent)
                .collect(Collectors.toList());

        List<String> generatedTags = aiTaggingService.generateTags(
                place.getName(),
                place.getFeatureText(),
                commentTexts
        );

        placeAiTagRepository.deleteByPlaceId(place.getId());
        
        List<PlaceAiTag> aiTags = generatedTags.stream()
                .map(tag -> new PlaceAiTag(place, tag))
                .collect(Collectors.toList());

        placeAiTagRepository.saveAll(aiTags);
        log.info("Successfully saved {} AI tags for place id: {}", aiTags.size(), place.getId());
    }
}
