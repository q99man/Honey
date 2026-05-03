package com.honeytong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.TrustGrade;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelHistoryRepository;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserGrowthServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private UserTrustRepository userTrustRepository;

    @Mock
    private UserLevelHistoryRepository userLevelHistoryRepository;

    @Mock
    private UserGrowthPolicyService userGrowthPolicyService;

    private UserGrowthService userGrowthService;
    private User user;

    @BeforeEach
    void setUp() {
        userGrowthService = new UserGrowthService(
                userRepository,
                userLevelRepository,
                userLevelHistoryRepository,
                userTrustRepository,
                userGrowthPolicyService
        );
        user = new User("tester", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void applyValidVisit_addsPolicyDrivenExpAndTrustScore() {
        UserLevel level = new UserLevel(user);
        UserTrust trust = new UserTrust(user);
        stubActiveUserAndPolicies(2, 1, OptionalInt.empty());
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.of(level));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userGrowthPolicyService.evaluateTrust(1))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        VisitGrowthResult result = userGrowthService.applyValidVisit(USER_ID);

        assertThat(result.expGained()).isEqualTo(2);
        assertThat(result.trustScoreDelta()).isEqualTo(1);
        assertThat(level.getExp()).isEqualTo(2);
        assertThat(level.getTotalExp()).isEqualTo(2);
        assertThat(trust.getTrustScore()).isEqualTo(1);
        assertThat(trust.getTrustGrade()).isEqualTo(TrustGrade.SEED_BEE);
        assertThat(trust.getRecommendWeight()).isEqualByComparingTo("1.00");
        assertThat(trust.getLastEvaluatedAt()).isNotNull();
        verify(userLevelRepository, never()).save(any(UserLevel.class));
        verify(userLevelHistoryRepository, never()).save(any());
        verify(userTrustRepository, never()).save(any(UserTrust.class));
    }

    @Test
    void applyValidVisit_initializesMissingLevelAndTrustRows() {
        stubActiveUserAndPolicies(3, 2, OptionalInt.empty());
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userLevelRepository.save(any(UserLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userTrustRepository.save(any(UserTrust.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGrowthPolicyService.evaluateTrust(2))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        VisitGrowthResult result = userGrowthService.applyValidVisit(USER_ID);

        assertThat(result.expGained()).isEqualTo(3);
        assertThat(result.trustScoreDelta()).isEqualTo(2);
        verify(userLevelRepository).save(any(UserLevel.class));
        verify(userTrustRepository).save(any(UserTrust.class));
    }

    @Test
    void applyValidVisit_advancesLevelAndWritesHistory() {
        UserLevel level = new UserLevel(user);
        UserTrust trust = new UserTrust(user);
        stubActiveUserAndPolicies(12, 1, OptionalInt.of(10));
        when(userGrowthPolicyService.getNextLevelExp(2)).thenReturn(OptionalInt.empty());
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.of(level));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userGrowthPolicyService.evaluateTrust(1))
                .thenReturn(new TrustEvaluationResult(TrustGrade.SEED_BEE, BigDecimal.valueOf(1.00)));

        VisitGrowthResult result = userGrowthService.applyValidVisit(USER_ID);

        assertThat(result.expGained()).isEqualTo(12);
        assertThat(level.getLevel()).isEqualTo(2);
        assertThat(level.getExp()).isEqualTo(2);
        assertThat(level.getTotalExp()).isEqualTo(12);
        verify(userLevelHistoryRepository).save(any());
    }

    @Test
    void applyValidVisit_appliesTrustGradeAndRecommendationWeight() {
        UserLevel level = new UserLevel(user);
        UserTrust trust = new UserTrust(user);
        stubActiveUserAndPolicies(2, 10, OptionalInt.empty());
        when(userLevelRepository.findById(USER_ID)).thenReturn(Optional.of(level));
        when(userTrustRepository.findById(USER_ID)).thenReturn(Optional.of(trust));
        when(userGrowthPolicyService.evaluateTrust(10))
                .thenReturn(new TrustEvaluationResult(TrustGrade.LOCAL_BEE, BigDecimal.valueOf(1.10)));

        userGrowthService.applyValidVisit(USER_ID);

        assertThat(trust.getTrustScore()).isEqualTo(10);
        assertThat(trust.getTrustGrade()).isEqualTo(TrustGrade.LOCAL_BEE);
        assertThat(trust.getRecommendWeight()).isEqualByComparingTo("1.10");
    }

    @Test
    void applyValidVisit_rejectsInvalidVisitExpPolicy() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userGrowthPolicyService.getVisitExp()).thenThrow(new ApiException(ErrorCode.POLICY_VIOLATION));

        assertThatThrownBy(() -> userGrowthService.applyValidVisit(USER_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POLICY_VIOLATION));
        verify(userLevelRepository, never()).findById(any());
        verify(userTrustRepository, never()).findById(any());
    }

    private void stubActiveUserAndPolicies(int visitExp, int trustScoreDelta, OptionalInt nextLevelExp) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userGrowthPolicyService.getVisitExp()).thenReturn(visitExp);
        when(userGrowthPolicyService.getValidVisitScore()).thenReturn(trustScoreDelta);
        when(userGrowthPolicyService.getNextLevelExp(1)).thenReturn(nextLevelExp);
    }
}
