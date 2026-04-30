package com.honeytong.comment.controller;

import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.comment.dto.CommentCreateResponse;
import com.honeytong.comment.dto.CommentDeleteResponse;
import com.honeytong.comment.dto.CommentRequest;
import com.honeytong.comment.dto.CommentResponse;
import com.honeytong.comment.service.CommentService;
import com.honeytong.common.api.ApiResponse;
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
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @RequirePhoneVerified
    @PostMapping("/places/{placeId}/comments")
    public ApiResponse<CommentCreateResponse> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ApiResponse.success(commentService.createComment(userId, placeId, request), "Comment created");
    }

    @RequirePhoneVerified
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ApiResponse.success(commentService.updateComment(userId, commentId, request), "Comment updated");
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<CommentDeleteResponse> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentService.deleteComment(userId, commentId), "Comment deleted");
    }

    @GetMapping("/places/{placeId}/comments")
    public ApiResponse<List<CommentResponse>> getPlaceComments(@PathVariable Long placeId) {
        return ApiResponse.success(commentService.getPlaceComments(placeId), "OK");
    }

    @GetMapping("/users/me/comments")
    public ApiResponse<List<CommentResponse>> getMyComments(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(commentService.getMyComments(userId), "OK");
    }
}
