package com.honeytong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.UserSanctionStatus;
import com.honeytong.user.entity.UserSanctionType;
import com.honeytong.user.repository.UserSanctionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ActiveSanctionGuardAspectTest {

    private static final long USER_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 12, 0);

    @Mock
    private UserSanctionRepository userSanctionRepository;

    private ActiveSanctionGuardAspect guardAspect;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T03:00:00Z"), ZONE);
        guardAspect = new ActiveSanctionGuardAspect(userSanctionRepository, clock);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireNoActiveSanction_allowsWhenNoBlockingSanctionExists() {
        when(userSanctionRepository.existsBlockingSanction(
                org.mockito.Mockito.eq(USER_ID),
                org.mockito.Mockito.eq(UserSanctionStatus.ACTIVE),
                org.mockito.Mockito.anyCollection(),
                org.mockito.Mockito.eq(NOW)
        )).thenReturn(false);

        guardAspect.requireNoActiveSanction();

        org.mockito.Mockito.verify(userSanctionRepository).existsBlockingSanction(
                org.mockito.Mockito.eq(USER_ID),
                org.mockito.Mockito.eq(UserSanctionStatus.ACTIVE),
                org.mockito.Mockito.argThat(types ->
                        types.contains(UserSanctionType.TEMPORARY_RESTRICTION)
                                && types.contains(UserSanctionType.PERMANENT_RESTRICTION)
                                && !types.contains(UserSanctionType.WARNING)
                                && types.size() == 2
                ),
                org.mockito.Mockito.eq(NOW)
        );
    }

    @Test
    void requireNoActiveSanction_rejectsBlockingSanction() {
        when(userSanctionRepository.existsBlockingSanction(
                org.mockito.Mockito.eq(USER_ID),
                org.mockito.Mockito.eq(UserSanctionStatus.ACTIVE),
                org.mockito.Mockito.anyCollection(),
                org.mockito.Mockito.eq(NOW)
        )).thenReturn(true);

        assertThatThrownBy(() -> guardAspect.requireNoActiveSanction())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_SANCTION_ACTIVE));
    }

    @Test
    void requireNoActiveSanction_requiresAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> guardAspect.requireNoActiveSanction())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
