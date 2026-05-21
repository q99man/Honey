package com.honeytong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.honeytong.auth.config.SecurityProperties;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private SecurityProperties securityProperties;
    private PolicyService policyService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties("test-honeytong-jwt-secret-key-for-unit-test-1234567890");
        policyService = mock(PolicyService.class);
        when(policyService.getRequiredInteger("auth", "jwt_access_token_minutes")).thenReturn(15);
        jwtTokenProvider = new JwtTokenProvider(securityProperties, policyService);
    }

    @Test
    void createAccessToken_containsUserIdAndRole() {
        User user = new User("테스터", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        String token = jwtTokenProvider.createAccessToken(user);
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        assertThat(jwtTokenProvider.isValidAccessToken(token)).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
