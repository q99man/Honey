package com.honeytong.ranking.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.ranking.dto.CurrentSeasonResponse;
import com.honeytong.ranking.dto.PlaceRankingResponse;
import com.honeytong.ranking.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/places")
    public ApiResponse<PlaceRankingResponse> getPlaceRankings(
            @RequestParam String regionType,
            @RequestParam Long regionId,
            @RequestParam(required = false) String seasonCode
    ) {
        return ApiResponse.success(rankingService.getPlaceRankings(regionType, regionId, seasonCode), "OK");
    }

    @GetMapping("/seasons/current")
    public ApiResponse<CurrentSeasonResponse> getCurrentSeason() {
        return ApiResponse.success(rankingService.getCurrentSeason(), "OK");
    }
}
