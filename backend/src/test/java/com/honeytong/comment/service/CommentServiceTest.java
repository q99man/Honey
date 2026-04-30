package com.honeytong.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.comment.dto.CommentRequest;
import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceRepository;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long PLACE_ID = 100L;
    private static final long COMMENT_ID = 500L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyService policyService;

    private CommentService commentService;
    private User user;
    private User otherUser;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                placeRepository,
                placeStatsRepository,
                userRepository,
                policyService
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        otherUser = new User("다른사용자", "other@example.com");
        ReflectionTestUtils.setField(otherUser, "id", OTHER_USER_ID);

        RegionCity city = new RegionCity("서울특별시", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "마포구", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "서교동", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);
        place = new Place(
                user,
                dong,
                "한정 국밥",
                "KOREAN",
                "서울 마포구 독막로 1",
                null,
                BigDecimal.valueOf(37.5500000),
                BigDecimal.valueOf(126.9100000),
                "10000_20000",
                "국밥",
                "동네에서 다시 찾고 싶은 국밥집",
                "맑은 국물과 빠른 회전이 좋습니다.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);
    }

    @Test
    void createComment_savesCommentAndUpdatesStats() {
        stubActiveUserAndVisiblePlace();
        when(commentRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
            return comment;
        });
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = commentService.createComment(
                USER_ID,
                PLACE_ID,
                new CommentRequest("  국물이 깔끔하고 다시 가고 싶어요.  ")
        );

        assertThat(response.commentId()).isEqualTo(COMMENT_ID);
        assertThat(stats.getCommentCount()).isEqualTo(1);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_rejectsDuplicateVisibleComment() {
        Comment existing = newComment("이미 작성한 댓글");
        stubActiveUserAndVisiblePlace();
        when(commentRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.createComment(
                USER_ID,
                PLACE_ID,
                new CommentRequest("새 댓글")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_ALREADY_EXISTS));
        verify(placeStatsRepository, never()).findById(any());
    }

    @Test
    void createComment_restoresDeletedComment() {
        Comment deleted = newComment("삭제된 댓글");
        deleted.delete();
        stubActiveUserAndVisiblePlace();
        when(commentRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).thenReturn(Optional.of(deleted));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = commentService.createComment(USER_ID, PLACE_ID, new CommentRequest("다시 작성한 댓글"));

        assertThat(response.commentId()).isEqualTo(COMMENT_ID);
        assertThat(deleted.isVisible()).isTrue();
        assertThat(deleted.getContent()).isEqualTo("다시 작성한 댓글");
        assertThat(stats.getCommentCount()).isEqualTo(1);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_updatesOwnComment() {
        Comment comment = newComment("기존 댓글");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        var response = commentService.updateComment(USER_ID, COMMENT_ID, new CommentRequest("수정한 댓글"));

        assertThat(response.content()).isEqualTo("수정한 댓글");
        assertThat(comment.getContent()).isEqualTo("수정한 댓글");
    }

    @Test
    void updateComment_rejectsNonOwner() {
        Comment comment = newComment("기존 댓글");
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                OTHER_USER_ID,
                COMMENT_ID,
                new CommentRequest("권한 없는 수정")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void deleteComment_deletesOwnCommentAndUpdatesStats() {
        Comment comment = newComment("삭제할 댓글");
        stats.addComment(BigDecimal.valueOf(0.5));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = commentService.deleteComment(USER_ID, COMMENT_ID);

        assertThat(response.deleted()).isTrue();
        assertThat(response.commentCount()).isZero();
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    @Test
    void getPlaceComments_returnsVisibleComments() {
        Comment comment = newComment("장소 댓글");
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(commentRepository.findByPlaceIdAndStatusOrderByCreatedAtDesc(PLACE_ID, CommentStatus.VISIBLE))
                .thenReturn(List.of(comment));

        var response = commentService.getPlaceComments(PLACE_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).content()).isEqualTo("장소 댓글");
    }

    @Test
    void getMyComments_returnsOwnVisibleComments() {
        Comment comment = newComment("내 댓글");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(commentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, CommentStatus.VISIBLE))
                .thenReturn(List.of(comment));

        var response = commentService.getMyComments(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).placeId()).isEqualTo(PLACE_ID);
        assertThat(response.get(0).content()).isEqualTo("내 댓글");
    }

    private void stubActiveUserAndVisiblePlace() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
    }

    private Comment newComment(String content) {
        Comment comment = new Comment(user, place, content);
        ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
        return comment;
    }
}
