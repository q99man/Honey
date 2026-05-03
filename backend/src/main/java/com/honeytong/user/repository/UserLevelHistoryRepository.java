package com.honeytong.user.repository;

import com.honeytong.user.entity.UserLevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLevelHistoryRepository extends JpaRepository<UserLevelHistory, Long> {
}
