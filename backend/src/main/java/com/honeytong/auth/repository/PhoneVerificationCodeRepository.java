package com.honeytong.auth.repository;

import com.honeytong.auth.entity.PhoneVerificationCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, Long> {

    Optional<PhoneVerificationCode> findTopByUserIdAndPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            Long userId,
            String phone
    );

    Optional<PhoneVerificationCode> findByIdAndUserIdAndPhoneAndVerifiedFalse(
            Long id,
            Long userId,
            String phone
    );
}
