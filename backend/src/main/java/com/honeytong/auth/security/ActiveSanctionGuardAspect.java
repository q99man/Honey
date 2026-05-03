package com.honeytong.auth.security;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.UserSanctionStatus;
import com.honeytong.user.entity.UserSanctionType;
import com.honeytong.user.repository.UserSanctionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ActiveSanctionGuardAspect {

    private static final List<UserSanctionType> BLOCKING_TYPES = List.of(
            UserSanctionType.TEMPORARY_RESTRICTION,
            UserSanctionType.PERMANENT_RESTRICTION
    );

    private final UserSanctionRepository userSanctionRepository;
    private final Clock clock;

    @Autowired
    public ActiveSanctionGuardAspect(UserSanctionRepository userSanctionRepository) {
        this(userSanctionRepository, Clock.systemDefaultZone());
    }

    ActiveSanctionGuardAspect(UserSanctionRepository userSanctionRepository, Clock clock) {
        this.userSanctionRepository = userSanctionRepository;
        this.clock = clock;
    }

    @Before("@within(com.honeytong.auth.security.RequireNoActiveSanction) || "
            + "@annotation(com.honeytong.auth.security.RequireNoActiveSanction)")
    public void requireNoActiveSanction() {
        Long userId = currentUserId();
        boolean hasBlockingSanction = userSanctionRepository.existsBlockingSanction(
                userId,
                UserSanctionStatus.ACTIVE,
                BLOCKING_TYPES,
                LocalDateTime.now(clock)
        );
        if (hasBlockingSanction) {
            throw new ApiException(ErrorCode.USER_SANCTION_ACTIVE);
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
