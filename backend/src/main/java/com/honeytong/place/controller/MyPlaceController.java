package com.honeytong.place.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.place.dto.PlaceListItemResponse;
import com.honeytong.place.service.PlaceService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/places")
public class MyPlaceController {

    private final PlaceService placeService;

    public MyPlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public ApiResponse<List<PlaceListItemResponse>> getMyRegisteredPlaces(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(placeService.getMyRegisteredPlaces(userId), "OK");
    }
}
