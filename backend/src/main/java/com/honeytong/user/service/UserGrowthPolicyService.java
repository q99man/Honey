package com.honeytong.user.service;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.TrustGrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;

@Service
public class UserGrowthPolicyService {

    private static final String GROWTH_POLICY_GROUP = "growth";
    private static final String VISIT_EXP_KEY = "visit_exp";
    private static final String LEVEL_EXP_THRESHOLDS_KEY = "level_exp_thresholds";
    private static final String TRUST_POLICY_GROUP = "trust";
    private static final String VALID_VISIT_SCORE_KEY = "valid_visit_score";
    private static final String GRADE_THRESHOLDS_KEY = "grade_thresholds";
    private static final String RECOMMEND_WEIGHT_BY_GRADE_KEY = "recommend_weight_by_grade";
    private static final int RECOMMEND_WEIGHT_INTEGER_DIGITS = 2;
    private static final int RECOMMEND_WEIGHT_FRACTION_DIGITS = 2;

    private final PolicyService policyService;

    public UserGrowthPolicyService(PolicyService policyService) {
        this.policyService = policyService;
    }

    public int getVisitExp() {
        return getNonNegativeIntegerPolicy(GROWTH_POLICY_GROUP, VISIT_EXP_KEY);
    }

    public int getValidVisitScore() {
        return getNonNegativeIntegerPolicy(TRUST_POLICY_GROUP, VALID_VISIT_SCORE_KEY);
    }

    public OptionalInt getNextLevelExp(int currentLevel) {
        if (currentLevel <= 0) {
            throw invalidPolicy("User level must be positive.");
        }
        Map<Integer, Integer> thresholds = parseLevelThresholds(policyService.getRequiredString(
                GROWTH_POLICY_GROUP,
                LEVEL_EXP_THRESHOLDS_KEY
        ));
        Integer threshold = thresholds.get(currentLevel);
        return threshold == null ? OptionalInt.empty() : OptionalInt.of(threshold);
    }

    public TrustEvaluationResult evaluateTrust(int trustScore) {
        Map<TrustGrade, Integer> gradeThresholds = parseTrustGradeThresholds(policyService.getRequiredString(
                TRUST_POLICY_GROUP,
                GRADE_THRESHOLDS_KEY
        ));
        Map<TrustGrade, BigDecimal> weights = parseTrustWeights(policyService.getRequiredString(
                TRUST_POLICY_GROUP,
                RECOMMEND_WEIGHT_BY_GRADE_KEY
        ));

        TrustGrade selectedGrade = TrustGrade.SEED_BEE;
        int selectedThreshold = Integer.MIN_VALUE;
        for (Map.Entry<TrustGrade, Integer> entry : gradeThresholds.entrySet()) {
            int threshold = entry.getValue();
            if (trustScore >= threshold && threshold >= selectedThreshold) {
                selectedGrade = entry.getKey();
                selectedThreshold = threshold;
            }
        }

        BigDecimal recommendWeight = weights.get(selectedGrade);
        if (recommendWeight == null) {
            throw invalidPolicy("Recommendation weight is missing for trust grade: " + selectedGrade.name());
        }
        return new TrustEvaluationResult(selectedGrade, recommendWeight);
    }

    private int getNonNegativeIntegerPolicy(String policyGroup, String policyKey) {
        int value = policyService.getRequiredInteger(policyGroup, policyKey);
        if (value < 0) {
            throw invalidPolicy("Policy must be zero or positive: " + policyGroup + "." + policyKey);
        }
        return value;
    }

    private Map<Integer, Integer> parseLevelThresholds(String policyValue) {
        Map<Integer, Integer> thresholds = new HashMap<>();
        for (String entry : splitEntries(policyValue)) {
            String[] parts = splitEntry(entry);
            int level = parsePositiveInteger(parts[0], "Level must be positive.");
            int requiredExp = parsePositiveInteger(parts[1], "Level threshold EXP must be positive.");
            if (thresholds.put(level, requiredExp) != null) {
                throw invalidPolicy("Duplicate level threshold: " + level);
            }
        }
        return thresholds;
    }

    private Map<TrustGrade, Integer> parseTrustGradeThresholds(String policyValue) {
        Map<TrustGrade, Integer> thresholds = new EnumMap<>(TrustGrade.class);
        for (String entry : splitEntries(policyValue)) {
            String[] parts = splitEntry(entry);
            TrustGrade grade = parseTrustGrade(parts[0]);
            int threshold = parseNonNegativeInteger(parts[1], "Trust grade threshold must be zero or positive.");
            if (thresholds.put(grade, threshold) != null) {
                throw invalidPolicy("Duplicate trust grade threshold: " + grade.name());
            }
        }
        return thresholds;
    }

    private Map<TrustGrade, BigDecimal> parseTrustWeights(String policyValue) {
        Map<TrustGrade, BigDecimal> weights = new EnumMap<>(TrustGrade.class);
        for (String entry : splitEntries(policyValue)) {
            String[] parts = splitEntry(entry);
            TrustGrade grade = parseTrustGrade(parts[0]);
            BigDecimal weight = parseRecommendWeight(parts[1]);
            if (weights.put(grade, weight) != null) {
                throw invalidPolicy("Duplicate recommendation weight: " + grade.name());
            }
        }
        return weights;
    }

    private String[] splitEntries(String policyValue) {
        if (policyValue == null || policyValue.isBlank()) {
            throw invalidPolicy("Policy value must not be blank.");
        }
        return policyValue.split(";");
    }

    private String[] splitEntry(String entry) {
        String[] parts = entry.trim().split(":");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalidPolicy("Policy entry must use key:value format: " + entry);
        }
        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    private TrustGrade parseTrustGrade(String value) {
        try {
            return TrustGrade.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalidPolicy("Unknown trust grade: " + value);
        }
    }

    private int parsePositiveInteger(String value, String message) {
        int parsed = parseInteger(value, message);
        if (parsed <= 0) {
            throw invalidPolicy(message);
        }
        return parsed;
    }

    private int parseNonNegativeInteger(String value, String message) {
        int parsed = parseInteger(value, message);
        if (parsed < 0) {
            throw invalidPolicy(message);
        }
        return parsed;
    }

    private int parseInteger(String value, String message) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalidPolicy(message);
        }
    }

    private BigDecimal parseRecommendWeight(String value) {
        try {
            BigDecimal weight = new BigDecimal(value);
            if (weight.signum() < 0) {
                throw invalidPolicy("Recommendation weight must be zero or positive.");
            }
            if (weight.scale() > RECOMMEND_WEIGHT_FRACTION_DIGITS) {
                throw invalidPolicy("Recommendation weight supports up to two decimal places.");
            }
            int integerDigits = weight.precision() - weight.scale();
            if (integerDigits > RECOMMEND_WEIGHT_INTEGER_DIGITS) {
                throw invalidPolicy("Recommendation weight is too large.");
            }
            return weight.setScale(RECOMMEND_WEIGHT_FRACTION_DIGITS, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException exception) {
            throw invalidPolicy("Recommendation weight must be decimal.");
        }
    }

    private ApiException invalidPolicy(String message) {
        return new ApiException(ErrorCode.POLICY_VIOLATION, message);
    }
}
