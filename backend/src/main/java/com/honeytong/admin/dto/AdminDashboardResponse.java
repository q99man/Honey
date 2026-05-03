package com.honeytong.admin.dto;

public record AdminDashboardResponse(
        long todayNewUsers,
        long todayRecommendations,
        long todayVisits,
        long pendingReports,
        long newPlaces
) {
}
