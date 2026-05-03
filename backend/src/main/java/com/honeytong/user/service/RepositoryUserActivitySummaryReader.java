package com.honeytong.user.service;

import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.user.dto.UserActivitySummaryResponse;
import com.honeytong.visit.repository.VisitRepository;
import org.springframework.stereotype.Component;

@Component
public class RepositoryUserActivitySummaryReader implements UserActivitySummaryReader {

    private final RecommendationRepository recommendationRepository;
    private final VisitRepository visitRepository;
    private final CommentRepository commentRepository;
    private final PlaceRepository placeRepository;

    public RepositoryUserActivitySummaryReader(
            RecommendationRepository recommendationRepository,
            VisitRepository visitRepository,
            CommentRepository commentRepository,
            PlaceRepository placeRepository
    ) {
        this.recommendationRepository = recommendationRepository;
        this.visitRepository = visitRepository;
        this.commentRepository = commentRepository;
        this.placeRepository = placeRepository;
    }

    @Override
    public UserActivitySummaryResponse read(Long userId) {
        return new UserActivitySummaryResponse(
                recommendationRepository.countByUserIdAndStatus(userId, RecommendationStatus.ACTIVE),
                visitRepository.countByUserIdAndValidTrue(userId),
                commentRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, CommentStatus.VISIBLE),
                placeRepository.countByCreatedByIdAndDeletedAtIsNull(userId)
        );
    }
}
