package com.honeytong.ranking.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.ranking.dto.AdminRankingHistoryFinalizeRequest;
import com.honeytong.ranking.dto.AdminRankingHistoryFinalizeResponse;
import com.honeytong.ranking.dto.AdminRankingPlaceExclusionRequest;
import com.honeytong.ranking.dto.AdminRankingPlaceExclusionResponse;
import com.honeytong.ranking.dto.AdminRankingRecalculateRequest;
import com.honeytong.ranking.dto.AdminRankingRecalculateResponse;
import com.honeytong.ranking.dto.AdminSeasonCreateRequest;
import com.honeytong.ranking.dto.AdminSeasonResponse;
import com.honeytong.ranking.dto.AdminSeasonUpdateRequest;
import com.honeytong.ranking.service.AdminRankingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminRankingController {

    private final AdminRankingService adminRankingService;

    public AdminRankingController(AdminRankingService adminRankingService) {
        this.adminRankingService = adminRankingService;
    }

    @GetMapping("/seasons")
    public ApiResponse<List<AdminSeasonResponse>> getSeasons(@AuthenticationPrincipal Long adminUserId) {
        return ApiResponse.success(adminRankingService.getSeasons(adminUserId), "OK");
    }

    @PostMapping("/seasons")
    public ApiResponse<AdminSeasonResponse> createSeason(
            @AuthenticationPrincipal Long adminUserId,
            @Valid @RequestBody AdminSeasonCreateRequest request
    ) {
        return ApiResponse.success(adminRankingService.createSeason(adminUserId, request), "Season created");
    }

    @PatchMapping("/seasons/{seasonId}")
    public ApiResponse<AdminSeasonResponse> updateSeason(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long seasonId,
            @Valid @RequestBody AdminSeasonUpdateRequest request
    ) {
        return ApiResponse.success(adminRankingService.updateSeason(adminUserId, seasonId, request), "Season updated");
    }

    @PostMapping("/rankings/recalculate")
    public ApiResponse<AdminRankingRecalculateResponse> recalculatePlaceRankings(
            @AuthenticationPrincipal Long adminUserId,
            @RequestBody(required = false) AdminRankingRecalculateRequest request
    ) {
        return ApiResponse.success(
                adminRankingService.recalculatePlaceRankings(adminUserId, request),
                "Ranking recalculated"
        );
    }

    @PostMapping("/rankings/seasons/{seasonId}/finalize-history")
    public ApiResponse<AdminRankingHistoryFinalizeResponse> finalizeRankingHistory(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long seasonId,
            @Valid @RequestBody(required = false) AdminRankingHistoryFinalizeRequest request
    ) {
        return ApiResponse.success(
                adminRankingService.finalizeRankingHistory(adminUserId, seasonId, request),
                "Ranking history finalized"
        );
    }

    @PatchMapping("/rankings/places/{placeId}/exclude")
    public ApiResponse<AdminRankingPlaceExclusionResponse> changePlaceRankingExclusion(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long placeId,
            @Valid @RequestBody AdminRankingPlaceExclusionRequest request
    ) {
        return ApiResponse.success(
                adminRankingService.changePlaceRankingExclusion(adminUserId, placeId, request),
                "Place ranking exclusion changed"
        );
    }
}
