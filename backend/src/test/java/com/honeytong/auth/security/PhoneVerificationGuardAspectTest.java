package com.honeytong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationGuardAspectTest {

    private static final long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    private PhoneVerificationGuardAspect guardAspect;

    @BeforeEach
    void setUp() {
        guardAspect = new PhoneVerificationGuardAspect(userRepository);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requirePhoneVerified_allowsVerifiedActiveUser() {
        User user = user(false);
        user.verifyPhone("01012345678");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        guardAspect.requirePhoneVerified();

        verify(userRepository).findById(USER_ID);
    }

    @Test
    void requirePhoneVerified_rejectsUnverifiedUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> guardAspect.requirePhoneVerified())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_VERIFICATION_REQUIRED));
    }

    @Test
    void requirePhoneVerified_rejectsDeletedUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> guardAspect.requirePhoneVerified())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requirePhoneVerified_requiresAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guardAspect.requirePhoneVerified())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private User user(boolean deleted) {
        User user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        if (deleted) {
            ReflectionTestUtils.setField(user, "deletedAt", java.time.LocalDateTime.now());
        }
        return user;
    }
}
