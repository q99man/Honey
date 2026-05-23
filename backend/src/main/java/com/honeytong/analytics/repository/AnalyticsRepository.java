package com.honeytong.analytics.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AnalyticsRepository {

    private final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findCategoryPreferences(Long userId) {
        String sql = "SELECT category_code, COUNT(*) as cnt " +
                "FROM (" +
                "  SELECT p.category_code FROM recommendations r JOIN places p ON r.place_id = p.id WHERE r.user_id = :userId " +
                "  UNION ALL " +
                "  SELECT p.category_code FROM visits v JOIN places p ON v.place_id = p.id WHERE v.user_id = :userId AND v.is_valid = TRUE " +
                "  UNION ALL " +
                "  SELECT p.category_code FROM comments c JOIN places p ON c.place_id = p.id WHERE c.user_id = :userId AND c.status = 'VISIBLE' " +
                ") combined " +
                "GROUP BY category_code " +
                "ORDER BY cnt DESC";
        return em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findPricePreferences(Long userId) {
        String sql = "SELECT price_range_code, COUNT(*) as cnt " +
                "FROM (" +
                "  SELECT p.price_range_code FROM recommendations r JOIN places p ON r.place_id = p.id WHERE r.user_id = :userId AND p.price_range_code IS NOT NULL " +
                "  UNION ALL " +
                "  SELECT p.price_range_code FROM visits v JOIN places p ON v.place_id = p.id WHERE v.user_id = :userId AND v.is_valid = TRUE AND p.price_range_code IS NOT NULL " +
                "  UNION ALL " +
                "  SELECT p.price_range_code FROM comments c JOIN places p ON c.place_id = p.id WHERE c.user_id = :userId AND c.status = 'VISIBLE' AND p.price_range_code IS NOT NULL " +
                ") combined " +
                "GROUP BY price_range_code " +
                "ORDER BY cnt DESC";
        return em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findUserRecommendationTrends(Long userId, LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM recommendations " +
                "WHERE user_id = :userId AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findUserVisitTrends(Long userId, LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM visits " +
                "WHERE user_id = :userId AND is_valid = TRUE AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findUserCommentTrends(Long userId, LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM comments " +
                "WHERE user_id = :userId AND status = 'VISIBLE' AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTrendingPlaces(Long dongId, LocalDateTime startDate, int limit) {
        String sql = "SELECT p.id, p.name, COUNT(*) as activity_count " +
                "FROM (" +
                "  SELECT place_id, created_at FROM recommendations " +
                "  UNION ALL " +
                "  SELECT place_id, created_at FROM visits WHERE is_valid = TRUE " +
                ") combined " +
                "JOIN places p ON combined.place_id = p.id " +
                "WHERE p.region_dong_id = :dongId AND combined.created_at >= :startDate AND p.deleted_at IS NULL AND p.exposure_status = 'VISIBLE' " +
                "GROUP BY p.id, p.name " +
                "ORDER BY activity_count DESC " +
                "LIMIT :limit";
        return em.createNativeQuery(sql)
                .setParameter("dongId", dongId)
                .setParameter("startDate", startDate)
                .setParameter("limit", limit)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findRisingCategories(Long dongId, LocalDateTime startDate) {
        String sql = "SELECT p.category_code, COUNT(*) as activity_count " +
                "FROM (" +
                "  SELECT place_id, created_at FROM recommendations " +
                "  UNION ALL " +
                "  SELECT place_id, created_at FROM visits WHERE is_valid = TRUE " +
                ") combined " +
                "JOIN places p ON combined.place_id = p.id " +
                "WHERE p.region_dong_id = :dongId AND combined.created_at >= :startDate AND p.deleted_at IS NULL AND p.exposure_status = 'VISIBLE' " +
                "GROUP BY p.category_code " +
                "ORDER BY activity_count DESC";
        return em.createNativeQuery(sql)
                .setParameter("dongId", dongId)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyNewUsers(LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM users " +
                "WHERE deleted_at IS NULL AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyNewPlaces(LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM places " +
                "WHERE deleted_at IS NULL AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyRecommendations(LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM recommendations " +
                "WHERE created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyVisits(LocalDateTime startDate) {
        String sql = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
                "FROM visits " +
                "WHERE is_valid = TRUE AND created_at >= :startDate " +
                "GROUP BY DATE(created_at)";
        return em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .getResultList();
    }
}
