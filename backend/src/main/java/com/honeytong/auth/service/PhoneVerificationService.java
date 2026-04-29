package com.honeytong.auth.service;

import com.honeytong.auth.config.PhoneVerificationProperties;
import com.honeytong.auth.dto.PhoneVerificationSendRequest;
import com.honeytong.auth.dto.PhoneVerificationSendResponse;
import com.honeytong.auth.dto.PhoneVerificationStatusResponse;
import com.honeytong.auth.dto.PhoneVerificationVerifyRequest;
import com.honeytong.auth.entity.PhoneVerificationCode;
import com.honeytong.auth.repository.PhoneVerificationCodeRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneVerificationService {

    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final PhoneVerificationCodeRepository phoneVerificationCodeRepository;
    private final PhoneVerificationSender phoneVerificationSender;
    private final PhoneVerificationProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            PhoneVerificationCodeRepository phoneVerificationCodeRepository,
            PhoneVerificationSender phoneVerificationSender,
            PhoneVerificationProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.phoneVerificationCodeRepository = phoneVerificationCodeRepository;
        this.phoneVerificationSender = phoneVerificationSender;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PhoneVerificationSendResponse sendCode(Long userId, PhoneVerificationSendRequest request) {
        User user = getActiveUser(userId);
        validatePhoneAvailable(request.phone(), userId);
        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(properties.codeTtlMinutes());

        phoneVerificationCodeRepository.save(
                new PhoneVerificationCode(user, request.phone(), codeHash, expiresAt)
        );
        phoneVerificationSender.send(request.phone(), code);

        return new PhoneVerificationSendResponse(true);
    }

    @Transactional
    public PhoneVerificationStatusResponse verifyCode(Long userId, PhoneVerificationVerifyRequest request) {
        User user = getActiveUser(userId);
        validatePhoneAvailable(request.phone(), userId);
        PhoneVerificationCode verificationCode = phoneVerificationCodeRepository
                .findTopByUserIdAndPhoneAndVerifiedFalseOrderByCreatedAtDesc(userId, request.phone())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드를 먼저 요청해 주세요."));

        validateAttemptAvailable(verificationCode);
        verificationCode.increaseAttempt();

        if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드가 올바르지 않습니다.");
        }

        verificationCode.markVerified();
        user.verifyPhone(request.phone());
        userTrustRepository.findById(userId).ifPresent(UserTrust::markPhoneVerified);

        return new PhoneVerificationStatusResponse(true);
    }

    @Transactional(readOnly = true)
    public PhoneVerificationStatusResponse getStatus(Long userId) {
        User user = getActiveUser(userId);
        return new PhoneVerificationStatusResponse(user.isPhoneVerified());
    }

    private void validateAttemptAvailable(PhoneVerificationCode verificationCode) {
        if (verificationCode.isExpired()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "만료된 인증 코드입니다.");
        }

        if (!verificationCode.canAttempt(properties.maxAttempts())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드 입력 횟수를 초과했습니다.");
        }
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private void validatePhoneAvailable(String phone, Long userId) {
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 전화번호입니다.");
        }
    }

    private String generateCode() {
        int length = properties.codeLength();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
