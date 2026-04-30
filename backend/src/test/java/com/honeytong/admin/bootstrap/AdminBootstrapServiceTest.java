package com.honeytong.admin.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import com.honeytong.auth.repository.UserAuthRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserRole;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void bootstrap_createsNewLocalSuperAdmin() {
        AdminBootstrapService service = newService(defaultProperties(false));
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, "admin@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 10L);
            return user;
        });
        when(userTrustRepository.existsById(10L)).thenReturn(false);
        when(userLevelRepository.existsById(10L)).thenReturn(false);

        AdminBootstrapResult result = service.bootstrap();

        assertThat(result.created()).isTrue();
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.role()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(result.passwordUpdated()).isTrue();

        ArgumentCaptor<UserAuth> authCaptor = ArgumentCaptor.forClass(UserAuth.class);
        verify(userAuthRepository).save(authCaptor.capture());
        assertThat(passwordEncoder.matches("local-password", authCaptor.getValue().getPasswordHash())).isTrue();

        ArgumentCaptor<UserTrust> trustCaptor = ArgumentCaptor.forClass(UserTrust.class);
        verify(userTrustRepository).save(trustCaptor.capture());
        assertThat(trustCaptor.getValue().isPhoneVerified()).isTrue();
        verify(userLevelRepository).save(any(UserLevel.class));
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void bootstrap_promotesExistingLocalUserAndResetsPasswordWhenConfigured() {
        AdminBootstrapProperties properties = defaultProperties(true);
        AdminBootstrapService service = newService(properties);
        User user = new User("existing", "admin@example.com");
        ReflectionTestUtils.setField(user, "id", 11L);
        UserAuth userAuth = new UserAuth(
                user,
                AuthProvider.LOCAL,
                "admin@example.com",
                "admin@example.com",
                passwordEncoder.encode("old-password")
        );
        when(userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, "admin@example.com"))
                .thenReturn(Optional.of(userAuth));
        when(userTrustRepository.existsById(11L)).thenReturn(true);
        when(userLevelRepository.existsById(11L)).thenReturn(true);

        AdminBootstrapResult result = service.bootstrap();

        assertThat(result.created()).isFalse();
        assertThat(result.passwordUpdated()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(passwordEncoder.matches("local-password", userAuth.getPasswordHash())).isTrue();
        verify(adminActionLogRepository).save(any(AdminActionLog.class));
    }

    @Test
    void bootstrap_rejectsNonAdminRole() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(
                true,
                "admin@example.com",
                "local-password",
                "local-admin",
                UserRole.USER,
                null,
                true,
                false
        );
        AdminBootstrapService service = newService(properties);

        assertThatThrownBy(service::bootstrap)
                .isInstanceOf(ApiException.class);
    }

    private AdminBootstrapService newService(AdminBootstrapProperties properties) {
        return new AdminBootstrapService(
                properties,
                userRepository,
                userAuthRepository,
                userTrustRepository,
                userLevelRepository,
                adminActionLogRepository,
                passwordEncoder
        );
    }

    private AdminBootstrapProperties defaultProperties(boolean resetPassword) {
        return new AdminBootstrapProperties(
                true,
                "admin@example.com",
                "local-password",
                "local-admin",
                UserRole.SUPER_ADMIN,
                "01012345678",
                true,
                resetPassword
        );
    }
}
