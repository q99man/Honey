package com.honeytong.visit.controller;

import com.honeytong.auth.security.RequireNoActiveSanction;
import com.honeytong.auth.security.RequirePhoneVerified;
import com.honeytong.common.api.ApiResponse;
import com.honeytong.visit.dto.MyVisitResponse;
import com.honeytong.visit.dto.PlaceVisitSummaryResponse;
import com.honeytong.visit.dto.VisitPolicyResponse;
import com.honeytong.visit.dto.VisitResponse;
import com.honeytong.visit.dto.VisitVerifyRequest;
import com.honeytong.visit.service.VisitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @RequirePhoneVerified
    @RequireNoActiveSanction
    @PostMapping("/places/{placeId}/visits")
    public ApiResponse<VisitResponse> verifyVisit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId,
            @Valid @RequestBody VisitVerifyRequest request
    ) {
        return ApiResponse.success(visitService.verifyVisit(userId, placeId, request), "Visit verified");
    }

    @GetMapping("/places/{placeId}/visit-policy")
    public ApiResponse<VisitPolicyResponse> getVisitPolicy(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long placeId
    ) {
        return ApiResponse.success(visitService.getVisitPolicy(userId, placeId), "OK");
    }

    @GetMapping("/users/me/visits")
    public ApiResponse<List<MyVisitResponse>> getMyVisits(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(visitService.getMyVisits(userId), "OK");
    }

    @GetMapping("/places/{placeId}/visits/summary")
    public ApiResponse<PlaceVisitSummaryResponse> getPlaceVisitSummary(@PathVariable Long placeId) {
        return ApiResponse.success(visitService.getPlaceVisitSummary(placeId), "OK");
    }
}
