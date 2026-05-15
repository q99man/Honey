package com.honeytong.community.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.community.dto.CommunityPostCreateResponse;
import com.honeytong.community.dto.CommunityPostDeleteResponse;
import com.honeytong.community.dto.CommunityPostRequest;
import com.honeytong.community.dto.CommunityPostResponse;
import com.honeytong.community.entity.CommunityPost;
import com.honeytong.community.entity.CommunityPostStatus;
import com.honeytong.community.repository.CommunityPostRepository;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.service.UserActionLogService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityPostService {

    private static final String COMMUNITY_POLICY_GROUP = "community";
    private static final String POST_TITLE_MAX_LENGTH_KEY = "post_title_max_length";
    private static final String POST_CONTENT_MAX_LENGTH_KEY = "post_content_max_length";
    private static final int POST_TITLE_COLUMN_LIMIT = 120;
    private static final int POST_CONTENT_COLUMN_LIMIT = 2000;

    private final CommunityPostRepository communityPostRepository;
    private final UserRepository userRepository;
    private final UserActionLogService userActionLogService;
    private final PolicyService policyService;

    public CommunityPostService(
            CommunityPostRepository communityPostRepository,
            UserRepository userRepository,
            UserActionLogService userActionLogService,
            PolicyService policyService
    ) {
        this.communityPostRepository = communityPostRepository;
        this.userRepository = userRepository;
        this.userActionLogService = userActionLogService;
        this.policyService = policyService;
    }

    @Transactional
    public CommunityPostCreateResponse createPost(Long userId, CommunityPostRequest request) {
        User user = getActiveUser(userId);
        CommunityPost post = communityPostRepository.save(new CommunityPost(
                user,
                normalizeTitle(request.title()),
                normalizeContent(request.content())
        ));
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMUNITY_POST_CREATE,
                UserActionLogService.TARGET_COMMUNITY_POST,
                post.getId(),
                Map.of()
        );
        return new CommunityPostCreateResponse(post.getId());
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, Long postId, CommunityPostRequest request) {
        User user = getActiveUser(userId);
        CommunityPost post = getVisiblePost(postId);
        validateAuthor(user.getId(), post);
        post.update(normalizeTitle(request.title()), normalizeContent(request.content()));
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMUNITY_POST_UPDATE,
                UserActionLogService.TARGET_COMMUNITY_POST,
                post.getId(),
                Map.of()
        );
        return toResponse(post, user.getId());
    }

    @Transactional
    public CommunityPostDeleteResponse deletePost(Long userId, Long postId) {
        User user = getActiveUser(userId);
        CommunityPost post = getVisiblePost(postId);
        validateAuthor(user.getId(), post);
        post.delete();
        userActionLogService.record(
                user.getId(),
                UserActionLogService.ACTION_COMMUNITY_POST_DELETE,
                UserActionLogService.TARGET_COMMUNITY_POST,
                post.getId(),
                Map.of()
        );
        return new CommunityPostDeleteResponse(post.getId(), true);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getPosts(Long viewerUserId) {
        return communityPostRepository.findByStatusOrderByCreatedAtDesc(CommunityPostStatus.VISIBLE)
                .stream()
                .filter(CommunityPost::isVisible)
                .map(post -> toResponse(post, viewerUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityPostResponse getPost(Long viewerUserId, Long postId) {
        return toResponse(getVisiblePost(postId), viewerUserId);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getMyPosts(Long userId) {
        User user = getActiveUser(userId);
        return communityPostRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), CommunityPostStatus.VISIBLE)
                .stream()
                .filter(CommunityPost::isVisible)
                .map(post -> toResponse(post, user.getId()))
                .toList();
    }

    private CommunityPost getVisiblePost(Long postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.isVisible()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private void validateAuthor(Long userId, CommunityPost post) {
        if (!post.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "게시글 작성자만 변경할 수 있습니다.");
        }
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "게시글 제목을 입력해 주세요.");
        }
        int maxLength = getBoundedMaxLength(POST_TITLE_MAX_LENGTH_KEY, POST_TITLE_COLUMN_LIMIT);
        if (normalized.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "게시글 제목 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "게시글 내용을 입력해 주세요.");
        }
        int maxLength = getBoundedMaxLength(POST_CONTENT_MAX_LENGTH_KEY, POST_CONTENT_COLUMN_LIMIT);
        if (normalized.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "게시글 내용 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    private int getBoundedMaxLength(String policyKey, int columnLimit) {
        int maxLength = policyService.getRequiredInteger(COMMUNITY_POLICY_GROUP, policyKey);
        if (maxLength <= 0 || maxLength > columnLimit) {
            throw new ApiException(
                    ErrorCode.POLICY_VIOLATION,
                    "게시글 길이 정책은 1-" + columnLimit + " 사이여야 합니다."
            );
        }
        return maxLength;
    }

    private CommunityPostResponse toResponse(CommunityPost post, Long viewerUserId) {
        User author = post.getUser();
        return new CommunityPostResponse(
                post.getId(),
                author.getId(),
                author.getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                viewerUserId != null && author.getId().equals(viewerUserId)
        );
    }
}
