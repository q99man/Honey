package com.honeytong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.dto.AdminCommentModerationRequest;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.comment.entity.Comment;
import com.honeytong.comment.entity.CommentStatus;
import com.honeytong.comment.repository.CommentRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.place.entity.Place;
import com.honeytong.place.entity.PlaceStats;
import com.honeytong.place.repository.PlaceStatsRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.region.entity.RegionCity;
import com.honeytong.region.entity.RegionDistrict;
import com.honeytong.region.entity.RegionDong;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
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
class AdminCommentServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long PLACE_ID = 100L;
    private static final long COMMENT_ID = 500L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PlaceStatsRepository placeStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    @Mock
    private PolicyService policyService;

    private AdminCommentService adminCommentService;
    private User admin;
    private User user;
    private Place place;
    private PlaceStats stats;

    @BeforeEach
    void setUp() {
        adminCommentService = new AdminCommentService(
                commentRepository,
                placeStatsRepository,
                userRepository,
                adminActionLogRepository,
                policyService,
                new ObjectMapper()
        );

        admin = new User("admin", "admin@example.com");
        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        user = new User("bee", "bee@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        RegionCity city = new RegionCity("Seoul", "Seoul", null, "11");
        ReflectionTestUtils.setField(city, "id", 10L);
        RegionDistrict district = new RegionDistrict(city, "Mapo-gu", "Mapo-gu", null, "11440");
        ReflectionTestUtils.setField(district, "id", 20L);
        RegionDong dong = new RegionDong(city, district, "Seogyo-dong", "Seogyo-dong", null, "1144066000");
        ReflectionTestUtils.setField(dong, "id", 30L);

        place = new Place(
                user,
                dong,
                "Local Soup",
                "KOREAN",
                "Road address",
                "Jibun address",
                BigDecimal.valueOf(37.55),
                BigDecimal.valueOf(126.91),
                "10000_20000",
                "Soup",
                "Worth revisiting.",
                "Deep broth.",
                false
        );
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        stats = new PlaceStats(place);
    }

    @Test
    void getComments_returnsLatestCommentLogs() {
        Comment comment = newComment("Good soup.");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(comment));

        var response = adminCommentService.getComments(ADMIN_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().commentId()).isEqualTo(COMMENT_ID);
        assertThat(response.getFirst().userId()).isEqualTo(USER_ID);
        assertThat(response.getFirst().nickname()).isEqualTo("bee");
        assertThat(response.getFirst().placeId()).isEqualTo(PLACE_ID);
        assertThat(response.getFirst().placeName()).isEqualTo("Local Soup");
        assertThat(response.getFirst().categoryCode()).isEqualTo("KOREAN");
        assertThat(response.getFirst().content()).isEqualTo("Good soup.");
        assertThat(response.getFirst().status()).isEqualTo(CommentStatus.VISIBLE);
    }

    @Test
    void blindComment_changesStatusSubtractsStatsAndLogsAction() {
        Comment comment = newComment("Spam comment.");
        stats.addComment(BigDecimal.valueOf(0.5));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = adminCommentService.blindComment(
                ADMIN_ID,
                COMMENT_ID,
                new AdminCommentModerationRequest("Hide after review.")
        );

        assertThat(response.status()).isEqualTo(CommentStatus.BLINDED);
        assertThat(response.commentCount()).isZero();
        assertThat(stats.getScoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void blindComment_doesNotLogWhenAlreadyNotVisible() {
        Comment comment = newComment("Already hidden.");
        comment.blind();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminCommentService.blindComment(
                ADMIN_ID,
                COMMENT_ID,
                new AdminCommentModerationRequest("No-op.")
        );

        assertThat(response.status()).isEqualTo(CommentStatus.BLINDED);
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void deleteComment_changesStatusSubtractsStatsAndLogsActionWhenVisible() {
        Comment comment = newComment("Delete me.");
        stats.addComment(BigDecimal.valueOf(0.5));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));
        when(policyService.getRequiredDecimal("ranking", "comment_weight")).thenReturn(BigDecimal.valueOf(0.5));

        var response = adminCommentService.deleteComment(
                ADMIN_ID,
                COMMENT_ID,
                new AdminCommentModerationRequest("Delete after review.")
        );

        assertThat(response.status()).isEqualTo(CommentStatus.DELETED);
        assertThat(response.commentCount()).isZero();
        assertThat(stats.getScoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void deleteComment_logsWithoutSubtractingStatsWhenAlreadyBlinded() {
        Comment comment = newComment("Hidden comment.");
        comment.blind();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminCommentService.deleteComment(
                ADMIN_ID,
                COMMENT_ID,
                new AdminCommentModerationRequest("Delete hidden comment.")
        );

        assertThat(response.status()).isEqualTo(CommentStatus.DELETED);
        assertThat(response.commentCount()).isZero();
        verify(policyService, never()).getRequiredDecimal("ranking", "comment_weight");
        verify(adminActionLogRepository).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void deleteComment_doesNotLogWhenAlreadyDeleted() {
        Comment comment = newComment("Deleted comment.");
        comment.delete();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(placeStatsRepository.findById(PLACE_ID)).thenReturn(Optional.of(stats));

        var response = adminCommentService.deleteComment(
                ADMIN_ID,
                COMMENT_ID,
                new AdminCommentModerationRequest("No-op.")
        );

        assertThat(response.status()).isEqualTo(CommentStatus.DELETED);
        verify(adminActionLogRepository, never()).save(org.mockito.Mockito.any(AdminActionLog.class));
    }

    @Test
    void getComments_rejectsNonAdmin() {
        User normalUser = new User("normal", "normal@example.com");
        ReflectionTestUtils.setField(normalUser, "id", 9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> adminCommentService.getComments(9L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private Comment newComment(String content) {
        Comment comment = new Comment(user, place, content);
        ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
        return comment;
    }
}
