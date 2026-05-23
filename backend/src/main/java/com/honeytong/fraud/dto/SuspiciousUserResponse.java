package com.honeytong.fraud.dto;

public record SuspiciousUserResponse(
        Long userId,
        String userNickname,
        long alertCount,
        double maxRiskScore
) {}
