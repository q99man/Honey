package com.honeytong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.recommendation.entity.RecommendationStatus;
import com.honeytong.recommendation.repository.RecommendationRepository;
import com.honeytong.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryUserActivitySummaryReaderTest {

    private static final long USER_ID = 1L;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PlaceRepository placeRepository;

    private RepositoryUserActivitySummaryReader reader;

    @BeforeEach
    void setUp() {
        reader = new RepositoryUserActivitySummaryReader(
                recommendationRepository,
                visitRepository,
                commentRepository,
                placeRepository
        );
    }

    @Test
    void read_returnsRepositoryBackedActivityCounts() {
        when(recommendationRepository.countByUserIdAndStatus(USER_ID, RecommendationStatus.ACTIVE)).thenReturn(3L);
        when(visitRepository.countByUserIdAndValidTrue(USER_ID)).thenReturn(4L);
        when(commentRepository.countByUserIdAndStatusAndDeletedAtIsNull(USER_ID, CommentStatus.VISIBLE))
                .thenReturn(5L);
        when(placeRepository.countByCreatedByIdAndDeletedAtIsNull(USER_ID)).thenReturn(6L);

        var response = reader.read(USER_ID);

        assertThat(response.recommendedCount()).isEqualTo(3);
        assertThat(response.visitCount()).isEqualTo(4);
        assertThat(response.commentCount()).isEqualTo(5);
        assertThat(response.registeredPlaceCount()).isEqualTo(6);
    }
}
