package com.honeytong.fraud.service;

import com.honeytong.fraud.entity.FraudAlert;
import com.honeytong.fraud.entity.FraudAlertType;
import com.honeytong.fraud.repository.FraudAlertRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import com.honeytong.user.repository.UserActionLogRepository;
import com.honeytong.user.service.UserGrowthPolicyService;
import com.honeytong.user.service.TrustEvaluationResult;
import com.honeytong.visit.entity.Visit;
import com.honeytong.visit.repository.VisitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final UserActionLogRepository userActionLogRepository;
    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserGrowthPolicyService userGrowthPolicyService;

    public FraudDetectionService(
            FraudAlertRepository fraudAlertRepository,
            UserActionLogRepository userActionLogRepository,
            VisitRepository visitRepository,
            UserRepository userRepository,
            UserTrustRepository userTrustRepository,
            UserGrowthPolicyService userGrowthPolicyService
    ) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.userActionLogRepository = userActionLogRepository;
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.userTrustRepository = userTrustRepository;
        this.userGrowthPolicyService = userGrowthPolicyService;
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditUserAction(Long userId, String actionType, String clientIp, Long targetId) {
        log.info("Auditing action for user {}: {}, IP: {}", userId, actionType, clientIp);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return;
        }

        // 1. RAPID_PARTICIPATION Check
        // 1분 이내 동일 유저의 행동 수 확인
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long rapidCount = userActionLogRepository.countByUserIdAndCreatedAtAfter(userId, oneMinuteAgo);
        if (rapidCount > 5) {
            String desc = String.format("단시간 다중 행동 감지 (1분 내 %d회 참여)", rapidCount);
            createAlert(user, FraudAlertType.RAPID_PARTICIPATION, 0.7, desc, clientIp, targetId);
        }

        // 2. IP_SPAM Check
        if (clientIp != null && !clientIp.isBlank()) {
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            long distinctUsers = userActionLogRepository.countDistinctUsersByIpAddressAndCreatedAtAfter(clientIp, tenMinutesAgo);
            if (distinctUsers >= 3) {
                String desc = String.format("동일 IP 다중 계정 스팸 감지 (10분 내 %d개 계정)", distinctUsers);
                createAlert(user, FraudAlertType.IP_SPAM, 0.8, desc, clientIp, targetId);
            }
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditVisitVerification(Long userId, Long placeId, String clientIp) {
        log.info("Auditing visit verification for user {}, place {}, IP: {}", userId, placeId, clientIp);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return;
        }

        // 1. GPS_TELEPORTATION Check
        List<Visit> userVisits = visitRepository.findByUserIdAndValidTrueOrderByCreatedAtDesc(userId);
        if (userVisits.size() < 2) {
            // 직전 방문이 없으므로 비교 불가
            return;
        }

        Visit currentVisit = userVisits.get(0);
        Visit prevVisit = userVisits.get(1);

        double lat1 = currentVisit.getLatitude().doubleValue();
        double lon1 = currentVisit.getLongitude().doubleValue();
        double lat2 = prevVisit.getLatitude().doubleValue();
        double lon2 = prevVisit.getLongitude().doubleValue();

        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        double timeDiffInSeconds = Duration.between(prevVisit.getCreatedAt(), currentVisit.getCreatedAt()).getSeconds();

        boolean isTeleport = false;
        double speed = 0;

        if (timeDiffInSeconds <= 0) {
            if (distance > 0.01) { // 10m 이상 이동
                isTeleport = true;
            }
        } else {
            speed = distance / (timeDiffInSeconds / 3600.0);
            if (speed > 150.0) {
                isTeleport = true;
            }
        }

        if (isTeleport) {
            String desc = String.format("GPS 조작 텔레포팅 감지 (이동 거리: %.2f km, 시간차: %d초, 속도: %.2f km/h)",
                    distance, (int) timeDiffInSeconds, speed);
            createAlert(user, FraudAlertType.GPS_TELEPORTATION, 0.9, desc, clientIp, currentVisit.getId());
        }
    }

    private void createAlert(User user, FraudAlertType alertType, double riskScore, String description, String ipAddress, Long targetId) {
        FraudAlert alert = new FraudAlert(user, alertType, riskScore, description, ipAddress, targetId);
        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created for user {}: {} [Risk: {}]", user.getId(), alertType, riskScore);

        // 신뢰 점수 차감 (-5점) 및 재평가
        UserTrust trust = userTrustRepository.findById(user.getId()).orElse(null);
        if (trust != null) {
            trust.addTrustScore(-5);
            TrustEvaluationResult result = userGrowthPolicyService.evaluateTrust(trust.getTrustScore());
            trust.applyTrustEvaluation(result.trustGrade(), result.recommendWeight());
            userTrustRepository.save(trust);
            log.info("User {} trust score reduced by 5 due to fraud alert. New score: {}", user.getId(), trust.getTrustScore());
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
