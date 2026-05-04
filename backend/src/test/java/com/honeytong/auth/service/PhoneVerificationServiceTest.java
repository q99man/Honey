package com.honeytong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.auth.config.PhoneVerificationProperties;
import com.honeytong.auth.dto.PhoneVerificationSendRequest;
import com.honeytong.auth.dto.PhoneVerificationVerifyRequest;
import com.honeytong.auth.entity.PhoneVerificationCode;
import com.honeytong.auth.repository.PhoneVerificationCodeRepository;
import com.honeytong.auth.verification.PhoneVerificationCache;
import com.honeytong.auth.verification.PhoneVerificationState;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    private static final long USER_ID = 1L;
    private static final long VERIFICATION_CODE_ID = 501L;
    private static final String PHONE = "01012345678";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private PhoneVerificationCodeRepository phoneVerificationCodeRepository;

    @Mock
    private PhoneVerificationCache phoneVerificationCache;

    @Captor
    private ArgumentCaptor<PhoneVerificationCode> codeCaptor;

    private CapturingPhoneVerificationSender sender;
    private PhoneVerificationService phoneVerificationService;
    private User user;
    private UserTrust userTrust;

    @BeforeEach
    void setUp() {
        sender = new CapturingPhoneVerificationSender();
        phoneVerificationService = new PhoneVerificationService(
                userRepository,
                userTrustRepository,
                phoneVerificationCodeRepository,
                sender,
                phoneVerificationCache,
                new PhoneVerificationProperties(6, 5, 5),
                new BCryptPasswordEncoder()
        );

        user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        userTrust = new UserTrust(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot(PHONE, USER_ID)).thenReturn(false);
    }

    @Test
    void sendCode_savesHashedCodeCachesStateAndSendsRawCode() {
        when(phoneVerificationCodeRepository.save(any(PhoneVerificationCode.class)))
                .thenAnswer(invocation -> {
                    PhoneVerificationCode verificationCode = invocation.getArgument(0);
                    ReflectionTestUtils.setField(verificationCode, "id", VERIFICATION_CODE_ID);
                    return verificationCode;
                });

        phoneVerificationService.sendCode(USER_ID, new PhoneVerificationSendRequest(PHONE));

        verify(phoneVerificationCodeRepository).save(codeCaptor.capture());
        verify(phoneVerificationCache).put(eq(USER_ID), eq(PHONE), any(PhoneVerificationState.class));
        assertThat(sender.phone).isEqualTo(PHONE);
        assertThat(sender.code).hasSize(6);
        assertThat(codeCaptor.getValue().getCodeHash()).isNotEqualTo(sender.code);
    }

    @Test
    void sendCode_doesNotCacheCodeWhenDeliveryFails() {
        sender.fail = true;
        when(phoneVerificationCodeRepository.save(any(PhoneVerificationCode.class)))
                .thenAnswer(invocation -> {
                    PhoneVerificationCode verificationCode = invocation.getArgument(0);
                    ReflectionTestUtils.setField(verificationCode, "id", VERIFICATION_CODE_ID);
                    return verificationCode;
                });

        assertThatThrownBy(() -> phoneVerificationService.sendCode(USER_ID, new PhoneVerificationSendRequest(PHONE)))
                .isInstanceOf(RuntimeException.class);

        verify(phoneVerificationCache, never()).put(eq(USER_ID), eq(PHONE), any(PhoneVerificationState.class));
    }

    @Test
    void verifyCode_marksUserAndTrustAsPhoneVerifiedAndEvictsCache() {
        PhoneVerificationCode verificationCode = issuedVerificationCode();
        when(phoneVerificationCache.getLatestUnverified(eq(USER_ID), eq(PHONE), any()))
                .thenReturn(Optional.of(PhoneVerificationState.from(verificationCode)));
        when(phoneVerificationCodeRepository.findByIdAndUserIdAndPhoneAndVerifiedFalse(
                VERIFICATION_CODE_ID,
                USER_ID,
                PHONE
        )).thenReturn(Optional.of(verificationCode));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));

        phoneVerificationService.verifyCode(
                USER_ID,
                new PhoneVerificationVerifyRequest(PHONE, sender.code)
        );

        assertThat(user.isPhoneVerified()).isTrue();
        verify(phoneVerificationCache).evict(USER_ID, PHONE);
    }

    private PhoneVerificationCode issuedVerificationCode() {
        when(phoneVerificationCodeRepository.save(any(PhoneVerificationCode.class)))
                .thenAnswer(invocation -> {
                    PhoneVerificationCode verificationCode = invocation.getArgument(0);
                    ReflectionTestUtils.setField(verificationCode, "id", VERIFICATION_CODE_ID);
                    return verificationCode;
                });
        phoneVerificationService.sendCode(USER_ID, new PhoneVerificationSendRequest(PHONE));
        verify(phoneVerificationCodeRepository).save(codeCaptor.capture());
        return codeCaptor.getValue();
    }

    private static class CapturingPhoneVerificationSender implements PhoneVerificationSender {

        private String phone;
        private String code;
        private boolean fail;

        @Override
        public void send(String phone, String code) {
            if (fail) {
                throw new RuntimeException("delivery failed");
            }
            this.phone = phone;
            this.code = code;
        }
    }
}
