package com.honeytong.fraud.controller;

import com.honeytong.common.api.ApiResponse;
import com.honeytong.fraud.dto.FraudAlertResponse;
import com.honeytong.fraud.dto.SuspiciousUserResponse;
import com.honeytong.fraud.repository.FraudAlertRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/fraud")
public class AdminFraudController {

    private final FraudAlertRepository fraudAlertRepository;

    public AdminFraudController(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @GetMapping("/alerts")
    public ApiResponse<List<FraudAlertResponse>> getFraudAlerts(@AuthenticationPrincipal Long adminUserId) {
        List<FraudAlertResponse> alerts = fraudAlertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(FraudAlertResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(alerts, "OK");
    }

    @GetMapping("/suspicious-users")
    public ApiResponse<List<SuspiciousUserResponse>> getSuspiciousUsers(@AuthenticationPrincipal Long adminUserId) {
        List<SuspiciousUserResponse> suspiciousUsers = fraudAlertRepository.findSuspiciousUsers()
                .stream()
                .map(row -> new SuspiciousUserResponse(
                        (Long) row[0],
                        (String) row[1],
                        (Long) row[2],
                        (Double) row[3]
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(suspiciousUsers, "OK");
    }
}
