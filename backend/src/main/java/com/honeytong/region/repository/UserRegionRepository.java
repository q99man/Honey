package com.honeytong.region.repository;

import com.honeytong.region.entity.UserRegion;
import com.honeytong.region.entity.UserRegionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    Optional<UserRegion> findByUserIdAndPrimaryRegionTrueAndStatus(Long userId, UserRegionStatus status);
}
