package com.honeytong.user.repository;

import com.honeytong.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(LocalDateTime from, LocalDateTime to);

    List<User> findAllByOrderByCreatedAtDesc();

    @Query(value = 
        "SELECT * FROM users u WHERE u.id IN (" +
        "  SELECT r.user_id FROM recommendations r WHERE r.place_id = :placeId AND r.status = 'ACTIVE' " +
        "  UNION " +
        "  SELECT v.user_id FROM visits v WHERE v.place_id = :placeId AND v.is_valid = true" +
        ")", nativeQuery = true)
    List<User> findDemographicsByPlaceId(@Param("placeId") Long placeId);
}
