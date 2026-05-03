package com.honeytong.user.repository;

import com.honeytong.user.entity.UserSanction;
import com.honeytong.user.entity.UserSanctionStatus;
import com.honeytong.user.entity.UserSanctionType;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    @Query("""
            select count(s) > 0
            from UserSanction s
            where s.user.id = :userId
              and s.status = :status
              and s.sanctionType in :blockingTypes
              and s.startAt <= :now
              and (s.endAt is null or s.endAt > :now)
            """)
    boolean existsBlockingSanction(
            @Param("userId") Long userId,
            @Param("status") UserSanctionStatus status,
            @Param("blockingTypes") Collection<UserSanctionType> blockingTypes,
            @Param("now") LocalDateTime now
    );
}
