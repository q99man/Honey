package com.honeytong.fraud.repository;

import com.honeytong.fraud.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    List<FraudAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT f.user.id, f.user.nickname, COUNT(f), MAX(f.riskScore) " +
           "FROM FraudAlert f " +
           "GROUP BY f.user.id, f.user.nickname " +
           "ORDER BY COUNT(f) DESC, MAX(f.riskScore) DESC")
    List<Object[]> findSuspiciousUsers();
}
