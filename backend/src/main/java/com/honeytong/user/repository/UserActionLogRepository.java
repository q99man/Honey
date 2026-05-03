package com.honeytong.user.repository;

import com.honeytong.user.entity.UserActionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {

    List<UserActionLog> findTop50ByOrderByCreatedAtDesc();
}
