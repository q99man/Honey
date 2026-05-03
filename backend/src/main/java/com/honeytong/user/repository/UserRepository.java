package com.honeytong.user.repository;

import com.honeytong.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(LocalDateTime from, LocalDateTime to);

    List<User> findAllByOrderByCreatedAtDesc();
}
