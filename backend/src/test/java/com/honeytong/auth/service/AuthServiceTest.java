package com.honeytong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.honeytong.auth.dto.OAuthLoginRequest;
import com.honeytong.auth.dto.LoginRequest;
import com.honeytong.auth.dto.SignupRequest;
import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import com.honeytong.auth.oauth.OAuthProviderClientRegistry;
import com.honeytong.auth.oauth.OAuthUserProfile;
import com.honeytong.auth.repository.UserAuthRepository;
import com.honeytong.auth.security.JwtTokenProvider;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserStatus;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OAuthProviderClientRegistry oauthProviderClientRegistry;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                userAuthRepository,
                userTrustRepository,
                userLevelRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenService,
                oauthProviderClientRegistry
        );
    }

    @Test
    void oauthLogin_createsUserAuthAndInitialGrowthRowsOnFirstLogin() {
        OAuthUserProfile profile = new OAuthUserProfile("kakao-1", "bee@example.com", "oauth-bee");
        when(oauthProviderClientRegistry.fetchUserProfile(AuthProvider.KAKAO, "provider-token"))
                .thenReturn(profile);
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 100L);
            return user;
        });
        when(userAuthRepository.save(any(UserAuth.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.createAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.issue(any(User.class))).thenReturn("refresh-token");

        var response = authService.oauthLogin("kakao", new OAuthLoginRequest("provider-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        verify(userAuthRepository).save(authCaptor.capture());
        assertThat(authCaptor.getValue().getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(authCaptor.getValue().getProviderUserId()).isEqualTo("kakao-1");
        assertThat(authCaptor.getValue().getEmail()).isEqualTo("bee@example.com");
        assertThat(authCaptor.getValue().getPasswordHash()).isNull();
        assertThat(authCaptor.getValue().getLastLoginAt()).isNotNull();

        verify(userTrustRepository).save(any(UserTrust.class));
        verify(userLevelRepository).save(any(UserLevel.class));
    }

    @Test
    void oauthLogin_issuesTokensForExistingLinkedAccount() {
        User user = new User("oauth-bee", "bee@example.com");
        ReflectionTestUtils.setField(user, "id", 101L);
        UserAuth userAuth = new UserAuth(user, AuthProvider.GOOGLE, "google-1", "bee@example.com", null);

        when(oauthProviderClientRegistry.fetchUserProfile(AuthProvider.GOOGLE, "provider-token"))
                .thenReturn(new OAuthUserProfile("google-1", "bee@example.com", "oauth-bee"));
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.of(userAuth));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        var response = authService.oauthLogin("GOOGLE", new OAuthLoginRequest("provider-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(userAuth.getLastLoginAt()).isNotNull();
        verify(userRepository, never()).save(any(User.class));
        verify(userTrustRepository, never()).save(any(UserTrust.class));
        verify(userLevelRepository, never()).save(any(UserLevel.class));
    }

    @Test
    void oauthLogin_rejectsLocalProvider() {
        assertThatThrownBy(() -> authService.oauthLogin("local", new OAuthLoginRequest("provider-token")))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(oauthProviderClientRegistry);
    }

    @Test
    void signup_failsWhenEmailAlreadyExists() {
        when(userAuthRepository.existsByProviderAndProviderUserId(AuthProvider.LOCAL, "bee@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("bee@example.com", "password", "nickname")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_failsWhenEmailOrPasswordIncorrect() {
        // Case 1: Email not found
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, "bee@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("bee@example.com", "password")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        // Case 2: Password incorrect
        User user = new User("nickname", "bee@example.com");
        UserAuth userAuth = new UserAuth(user, AuthProvider.LOCAL, "bee@example.com", "bee@example.com", "hashed_password");
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, "bee@example.com"))
                .thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bee@example.com", "wrong_password")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void login_failsWhenUserInactive() {
        User user = new User("nickname", "bee@example.com");
        ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
        UserAuth userAuth = new UserAuth(user, AuthProvider.LOCAL, "bee@example.com", "bee@example.com", "hashed_password");

        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, "bee@example.com"))
                .thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bee@example.com", "password")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
