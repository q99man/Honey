package com.honeytong.user.repository;

import com.honeytong.user.entity.UserActionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {

    List<UserActionLog> findTop50ByOrderByCreatedAtDesc();

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime dateTime);

    @Query("SELECT COUNT(DISTINCT u.user.id) FROM UserActionLog u WHERE u.ipAddress = :ipAddress AND u.createdAt >= :dateTime")
    long countDistinctUsersByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime dateTime);
}
