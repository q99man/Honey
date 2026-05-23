package com.honeytong.fraud.dto;

import com.honeytong.fraud.entity.FraudAlert;
import java.time.LocalDateTime;

public record FraudAlertResponse(
        Long id,
        Long userId,
        String userNickname,
        String alertType,
        double riskScore,
        String description,
        String ipAddress,
        Long targetId,
        LocalDateTime createdAt
) {
    public static FraudAlertResponse from(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getUser().getNickname(),
                alert.getAlertType().name(),
                alert.getRiskScore(),
                alert.getDescription(),
                alert.getIpAddress(),
                alert.getTargetId(),
                alert.getCreatedAt()
        );
    }
}
