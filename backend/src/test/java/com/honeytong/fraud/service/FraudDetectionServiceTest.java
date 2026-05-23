package com.honeytong.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.honeytong.fraud.entity.FraudAlert;
import com.honeytong.fraud.entity.FraudAlertType;
import com.honeytong.fraud.repository.FraudAlertRepository;
import com.honeytong.user.entity.TrustGrade;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import com.honeytong.user.repository.UserActionLogRepository;
import com.honeytong.user.service.UserGrowthPolicyService;
import com.honeytong.user.service.TrustEvaluationResult;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.repository.VisitRepository;
import com.honeytong.place.entity.Place;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private UserActionLogRepository userActionLogRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserGrowthPolicyService userGrowthPolicyService;

    private FraudDetectionService fraudDetectionService;
    private User user;
    private UserTrust userTrust;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(
                fraudAlertRepository,
                userActionLogRepository,
                visitRepository,
                userRepository,
                userTrustRepository,
                userGrowthPolicyService
        );
        user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        userTrust = new UserTrust(user);
        ReflectionTestUtils.setField(userTrust, "trustScore", 10);
    }

    @Test
    void auditUserAction_flagsRapidParticipation_whenCountExceedsFive() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userActionLogRepository.countByUserIdAndCreatedAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(6L); // 6 actions in 1 minute
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));
        when(userGrowthPolicyService.evaluateTrust(anyInt()))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        fraudDetectionService.auditUserAction(USER_ID, "COMMENT_CREATE", "127.0.0.1", 100L);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(alertCaptor.capture());
        
        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getAlertType()).isEqualTo(FraudAlertType.RAPID_PARTICIPATION);
        assertThat(alert.getRiskScore()).isEqualTo(0.7);
        assertThat(alert.getUser().getId()).isEqualTo(USER_ID);
        assertThat(userTrust.getTrustScore()).isEqualTo(5); // 10 - 5
    }

    @Test
    void auditUserAction_flagsIpSpam_whenDistinctUsersExceedTwo() {
        String testIp = "192.168.1.10";
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userActionLogRepository.countByUserIdAndCreatedAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(1L); // Not rapid
        when(userActionLogRepository.countDistinctUsersByIpAddressAndCreatedAtAfter(eq(testIp), any(LocalDateTime.class)))
                .thenReturn(3L); // 3 distinct users on same IP
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));
        when(userGrowthPolicyService.evaluateTrust(anyInt()))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        fraudDetectionService.auditUserAction(USER_ID, "RECOMMENDATION_CREATE", testIp, 100L);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(alertCaptor.capture());
        
        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getAlertType()).isEqualTo(FraudAlertType.IP_SPAM);
        assertThat(alert.getRiskScore()).isEqualTo(0.8);
        assertThat(userTrust.getTrustScore()).isEqualTo(5);
    }

    @Test
    void auditVisitVerification_flagsGpsTeleportation_whenSpeedExceeds150() {
        Place place1 = mock(Place.class);
        Place place2 = mock(Place.class);
        
        // Seoul Center (lat: 37.5665, lon: 126.9780)
        Visit currentVisit = new Visit(user, place1, BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780), 0, null, true, "VALID");
        ReflectionTestUtils.setField(currentVisit, "createdAt", LocalDateTime.now());
        
        // Busan Center (lat: 35.1796, lon: 129.0756) -> Distance ~ 325km
        Visit prevVisit = new Visit(user, place2, BigDecimal.valueOf(35.1796), BigDecimal.valueOf(129.0756), 0, null, true, "VALID");
        // Time difference: 10 minutes ago
        ReflectionTestUtils.setField(prevVisit, "createdAt", LocalDateTime.now().minusMinutes(10));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(visitRepository.findByUserIdAndValidTrueOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Arrays.asList(currentVisit, prevVisit));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(userTrust));
        when(userGrowthPolicyService.evaluateTrust(anyInt()))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        fraudDetectionService.auditVisitVerification(USER_ID, 100L, "127.0.0.1");

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(alertCaptor.capture());
        
        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getAlertType()).isEqualTo(FraudAlertType.GPS_TELEPORTATION);
        assertThat(alert.getRiskScore()).isEqualTo(0.9);
        assertThat(userTrust.getTrustScore()).isEqualTo(5);
    }
}
