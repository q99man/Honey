package com.honeytong.admin.controller;

import com.honeytong.admin.dto.AdminCommentModerationRequest;
import com.honeytong.admin.dto.AdminCommentModerationResponse;
import com.honeytong.admin.dto.AdminCommentResponse;
import com.honeytong.admin.service.AdminCommentService;
import com.honeytong.common.api.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    public ApiResponse<List<AdminCommentResponse>> getComments(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminCommentService.getComments(adminUserId), "OK");
    }

    @PatchMapping("/{commentId}/blind")
    public ApiResponse<AdminCommentModerationResponse> blindComment(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminCommentModerationRequest request
    ) {
        return ApiResponse.success(
                adminCommentService.blindComment(adminUserId, commentId, request),
                "Comment blinded"
        );
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<AdminCommentModerationResponse> deleteComment(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminCommentModerationRequest request
    ) {
        return ApiResponse.success(
                adminCommentService.deleteComment(adminUserId, commentId, request),
                "Comment deleted"
        );
    }
}
