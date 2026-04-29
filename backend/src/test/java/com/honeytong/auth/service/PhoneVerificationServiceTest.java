package com.honeytong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.honeytong.auth.config.PhoneVerificationProperties;
import com.honeytong.auth.dto.PhoneVerificationSendRequest;
import com.honeytong.auth.dto.PhoneVerificationVerifyRequest;
import com.honeytong.auth.entity.PhoneVerificationCode;
import com.honeytong.auth.repository.PhoneVerificationCodeRepository;
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
    private static final String PHONE = "01012345678";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private PhoneVerificationCodeRepository phoneVerificationCodeRepository;

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
                new PhoneVerificationProperties(6, 5, 5),
                new BCryptPasswordEncoder()
        );

        user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        userTrust = new UserTrust(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot(PHONE, USER_ID)).thenReturn(false);
    }

    @Test
    void sendCode_savesHashedCodeAndSendsRawCode() {
        phoneVerificationService.sendCode(USER_ID, new PhoneVerificationSendRequest(PHONE));

        org.mockito.Mockito.verify(phoneVerificationCodeRepository).save(codeCaptor.capture());
        assertThat(sender.phone).isEqualTo(PHONE);
        assertThat(sender.code).hasSize(6);
        assertThat(codeCaptor.getValue().getCodeHash()).isNotEqualTo(sender.code);
    }

    @Test
    void verifyCode_marksUserAndTrustAsPhoneVerified() {
        when(phoneVerificationCodeRepository.save(any(PhoneVerificationCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        phoneVerificationService.sendCode(USER_ID, new PhoneVerificationSendRequest(PHONE));
        org.mockito.Mockito.verify(phoneVerificationCodeRepository).save(codeCaptor.capture());

        when(phoneVerificationCodeRepository.findTopByUserIdAndPhoneAndVerifiedFalseOrderByCreatedAtDesc(USER_ID, PHONE))
                .thenReturn(Optional.of(codeCaptor.getValue()));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));

        phoneVerificationService.verifyCode(
                USER_ID,
                new PhoneVerificationVerifyRequest(PHONE, sender.code)
        );

        assertThat(user.isPhoneVerified()).isTrue();
    }

    private static class CapturingPhoneVerificationSender implements PhoneVerificationSender {

        private String phone;
        private String code;

        @Override
        public void send(String phone, String code) {
            this.phone = phone;
            this.code = code;
        }
    }
}
