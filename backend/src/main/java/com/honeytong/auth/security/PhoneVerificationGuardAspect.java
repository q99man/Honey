package com.honeytong.auth.security;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PhoneVerificationGuardAspect {

    private final UserRepository userRepository;

    public PhoneVerificationGuardAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("@within(com.honeytong.auth.security.RequirePhoneVerified) || "
            + "@annotation(com.honeytong.auth.security.RequirePhoneVerified)")
    public void requirePhoneVerified() {
        Long userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }

        if (!user.isPhoneVerified()) {
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_REQUIRED);
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
