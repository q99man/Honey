package com.honeytong.auth.service;

import com.honeytong.auth.dto.PhoneVerificationSendRequest;
import com.honeytong.auth.dto.PhoneVerificationSendResponse;
import com.honeytong.auth.dto.PhoneVerificationStatusResponse;
import com.honeytong.auth.dto.PhoneVerificationVerifyRequest;
import com.honeytong.auth.entity.PhoneVerificationCode;
import com.honeytong.auth.repository.PhoneVerificationCodeRepository;
import com.honeytong.auth.verification.PhoneVerificationCache;
import com.honeytong.auth.verification.PhoneVerificationState;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
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
    private final PhoneVerificationCache phoneVerificationCache;
    private final PolicyService policyService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            PhoneVerificationCodeRepository phoneVerificationCodeRepository,
            PhoneVerificationSender phoneVerificationSender,
            PhoneVerificationCache phoneVerificationCache,
            PolicyService policyService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.phoneVerificationCodeRepository = phoneVerificationCodeRepository;
        this.phoneVerificationSender = phoneVerificationSender;
        this.phoneVerificationCache = phoneVerificationCache;
        this.policyService = policyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PhoneVerificationSendResponse sendCode(Long userId, PhoneVerificationSendRequest request) {
        User user = getActiveUser(userId);
        validatePhoneAvailable(request.phone(), userId);
        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(getCodeTtlMinutes());

        PhoneVerificationCode verificationCode = new PhoneVerificationCode(
                user,
                request.phone(),
                codeHash,
                expiresAt
        );
        phoneVerificationCodeRepository.save(verificationCode);
        phoneVerificationSender.send(request.phone(), code);
        phoneVerificationCache.put(userId, request.phone(), PhoneVerificationState.from(verificationCode));

        return new PhoneVerificationSendResponse(true);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public PhoneVerificationStatusResponse verifyCode(Long userId, PhoneVerificationVerifyRequest request) {
        User user = getActiveUser(userId);
        validatePhoneAvailable(request.phone(), userId);

        PhoneVerificationState verificationState = phoneVerificationCache.getLatestUnverified(
                        userId,
                        request.phone(),
                        () -> phoneVerificationCodeRepository
                                .findTopByUserIdAndPhoneAndVerifiedFalseOrderByCreatedAtDesc(userId, request.phone())
                                .map(PhoneVerificationState::from)
                )
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드를 먼저 요청해 주세요."));

        validateAttemptAvailable(verificationState);
        PhoneVerificationCode verificationCode = getVerificationCode(userId, request.phone(), verificationState);
        validateAttemptAvailable(verificationCode);
        verificationCode.increaseAttempt();
        phoneVerificationCache.put(userId, request.phone(), PhoneVerificationState.from(verificationCode));

        if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드가 올바르지 않습니다.");
        }

        verificationCode.markVerified();
        user.verifyPhone(request.phone());
        userTrustRepository.findById(userId).ifPresent(UserTrust::markPhoneVerified);
        phoneVerificationCache.evict(userId, request.phone());

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

        if (!verificationCode.canAttempt(getMaxAttempts())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드 입력 횟수를 초과했습니다.");
        }
    }

    private void validateAttemptAvailable(PhoneVerificationState verificationState) {
        if (verificationState.isExpired()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "만료된 인증 코드입니다.");
        }

        if (!verificationState.canAttempt(getMaxAttempts())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드 입력 횟수를 초과했습니다.");
        }
    }

    private PhoneVerificationCode getVerificationCode(
            Long userId,
            String phone,
            PhoneVerificationState verificationState
    ) {
        if (verificationState.verificationCodeId() != null) {
            return phoneVerificationCodeRepository.findByIdAndUserIdAndPhoneAndVerifiedFalse(
                            verificationState.verificationCodeId(),
                            userId,
                            phone
                    )
                    .orElseThrow(() -> {
                        phoneVerificationCache.evict(userId, phone);
                        return new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드를 먼저 요청해 주세요.");
                    });
        }
        return phoneVerificationCodeRepository
                .findTopByUserIdAndPhoneAndVerifiedFalseOrderByCreatedAtDesc(userId, phone)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "인증 코드를 먼저 요청해 주세요."));
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

    private int getCodeLength() {
        int length = policyService.getRequiredInteger("auth", "phone_verification_code_length");
        if (length < 4 || length > 8) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "인증번호 길이는 4자 이상 8자 이하여야 합니다.");
        }
        return length;
    }

    private int getCodeTtlMinutes() {
        int ttl = policyService.getRequiredInteger("auth", "phone_verification_code_ttl_minutes");
        if (ttl < 1 || ttl > 60) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "인증번호 유효 시간은 1분 이상 60분 이하여야 합니다.");
        }
        return ttl;
    }

    private int getMaxAttempts() {
        int max = policyService.getRequiredInteger("auth", "phone_verification_max_attempts");
        if (max < 1 || max > 20) {
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "최대 인증 시도 횟수는 1회 이상 20회 이하여야 합니다.");
        }
        return max;
    }

    private String generateCode() {
        int length = getCodeLength();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
