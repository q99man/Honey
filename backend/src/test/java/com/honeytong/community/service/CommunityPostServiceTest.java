package com.honeytong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.community.dto.CommunityPostRequest;
import com.honeytong.community.entity.CommunityPost;
import com.honeytong.community.entity.CommunityPostStatus;
import com.honeytong.community.repository.CommunityPostRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserStatus;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long POST_ID = 100L;

    @Mock
    private CommunityPostRepository communityPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserActionLogService userActionLogService;

    private CommunityPostService communityPostService;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        communityPostService = new CommunityPostService(
                communityPostRepository,
                userRepository,
                userActionLogService
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        otherUser = new User("다른사용자", "other@example.com");
        ReflectionTestUtils.setField(otherUser, "id", OTHER_USER_ID);
    }

    @Test
    void createPost_savesPostForActiveUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityPostRepository.save(any(CommunityPost.class))).thenAnswer(invocation -> {
            CommunityPost post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", POST_ID);
            return post;
        });

        var response = communityPostService.createPost(
                USER_ID,
                new CommunityPostRequest("  동네 소식  ", "  오늘 새 가게가 열었어요.  ")
        );

        assertThat(response.postId()).isEqualTo(POST_ID);
        verify(communityPostRepository).save(any(CommunityPost.class));
        verify(userActionLogService).record(
                org.mockito.Mockito.eq(USER_ID),
                org.mockito.Mockito.eq(UserActionLogService.ACTION_COMMUNITY_POST_CREATE),
                org.mockito.Mockito.eq(UserActionLogService.TARGET_COMMUNITY_POST),
                org.mockito.Mockito.eq(POST_ID),
                org.mockito.Mockito.any()
        );
    }

    @Test
    void createPost_rejectsInactiveUser() {
        ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> communityPostService.createPost(
                USER_ID,
                new CommunityPostRequest("제목", "내용")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(communityPostRepository, never()).save(any());
    }

    @Test
    void getPosts_returnsVisiblePostsNewestFirst() {
        CommunityPost post = newPost(user, "동네 이야기", "함께 나눌 내용");
        when(communityPostRepository.findByStatusOrderByCreatedAtDesc(CommunityPostStatus.VISIBLE))
                .thenReturn(List.of(post));

        var response = communityPostService.getPosts(null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().postId()).isEqualTo(POST_ID);
        assertThat(response.getFirst().title()).isEqualTo("동네 이야기");
        assertThat(response.getFirst().mine()).isFalse();
    }

    @Test
    void updatePost_updatesOwnPost() {
        CommunityPost post = newPost(user, "기존 제목", "기존 내용");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        var response = communityPostService.updatePost(
                USER_ID,
                POST_ID,
                new CommunityPostRequest("수정 제목", "수정 내용")
        );

        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.content()).isEqualTo("수정 내용");
        assertThat(post.getTitle()).isEqualTo("수정 제목");
    }

    @Test
    void updatePost_rejectsNonAuthor() {
        CommunityPost post = newPost(user, "기존 제목", "기존 내용");
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(communityPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> communityPostService.updatePost(
                OTHER_USER_ID,
                POST_ID,
                new CommunityPostRequest("권한 없는 수정", "내용")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void deletePost_deletesOwnPost() {
        CommunityPost post = newPost(user, "삭제할 제목", "삭제할 내용");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(communityPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        var response = communityPostService.deletePost(USER_ID, POST_ID);

        assertThat(response.deleted()).isTrue();
        assertThat(post.getStatus()).isEqualTo(CommunityPostStatus.DELETED);
    }

    @Test
    void getPost_rejectsDeletedPost() {
        CommunityPost post = newPost(user, "삭제된 제목", "삭제된 내용");
        post.delete();
        when(communityPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> communityPostService.getPost(USER_ID, POST_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private CommunityPost newPost(User author, String title, String content) {
        CommunityPost post = new CommunityPost(author, title, content);
        ReflectionTestUtils.setField(post, "id", POST_ID);
        return post;
    }
}
