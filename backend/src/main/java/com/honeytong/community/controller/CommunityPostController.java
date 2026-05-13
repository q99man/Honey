package com.honeytong.community.controller;

import com.honeytong.auth.security.RequireNoActiveSanction;
import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.common.api.ApiResponse;
import com.honeytong.community.dto.CommunityPostCreateResponse;
import com.honeytong.community.dto.CommunityPostDeleteResponse;
import com.honeytong.community.dto.CommunityPostRequest;
import com.honeytong.community.dto.CommunityPostResponse;
import com.honeytong.community.service.CommunityPostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommunityPostController {

    private final CommunityPostService communityPostService;

    public CommunityPostController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @GetMapping("/community/posts")
    public ApiResponse<List<CommunityPostResponse>> getPosts(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(communityPostService.getPosts(userId), "OK");
    }

    @GetMapping("/community/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(communityPostService.getPost(userId, postId), "OK");
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @PostMapping("/community/posts")
    public ApiResponse<CommunityPostCreateResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return ApiResponse.success(communityPostService.createPost(userId, request), "Community post created");
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @PatchMapping("/community/posts/{postId}")
    public ApiResponse<CommunityPostResponse> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return ApiResponse.success(communityPostService.updatePost(userId, postId, request), "Community post updated");
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @DeleteMapping("/community/posts/{postId}")
    public ApiResponse<CommunityPostDeleteResponse> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.success(communityPostService.deletePost(userId, postId), "Community post deleted");
    }

    @GetMapping("/users/me/community-posts")
    public ApiResponse<List<CommunityPostResponse>> getMyPosts(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(communityPostService.getMyPosts(userId), "OK");
    }
}
