package com.honeytong.visit.dto;

public record VisitResponse(
        boolean verified,
        int distanceMeter,
        int expGained,
        int visitCount
) {
}
