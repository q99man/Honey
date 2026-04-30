package com.honeytong.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.dto.AdminPolicyResponse;
import com.honeytong.policy.dto.AdminPolicyUpdateRequest;
import com.honeytong.policy.dto.AdminRegionPolicyRequest;
import com.honeytong.policy.dto.AdminRegionPolicyResponse;
import com.honeytong.policy.entity.PolicyValueType;
import com.honeytong.policy.entity.SystemPolicy;
import com.honeytong.policy.repository.SystemPolicyRepository;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPolicyService {

    private static final String REGION_GROUP = "region";
    private static final String REGION_CHANGE_COOLDOWN_KEY = "change_cooldown_day";
    private static final String REGION_REGISTRATION_SCOPE_KEY = "registration_scope";
    private static final String POLICY_TARGET_TYPE = "SYSTEM_POLICY";
    private static final String POLICY_UPDATE_ACTION = "POLICY_UPDATE";

    private final SystemPolicyRepository systemPolicyRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    public AdminPolicyService(
            SystemPolicyRepository systemPolicyRepository,
            UserRepository userRepository,
            AdminActionLogRepository adminActionLogRepository,
            PolicyService policyService,
            ObjectMapper objectMapper
    ) {
        this.systemPolicyRepository = systemPolicyRepository;
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminPolicyResponse> getPolicies(Long adminUserId) {
        ensureAdmin(adminUserId);
        return systemPolicyRepository.findAllByOrderByPolicyGroupAscPolicyKeyAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminPolicyResponse updatePolicy(Long adminUserId, String fullPolicyKey, AdminPolicyUpdateRequest request) {
        User admin = ensureSuperAdmin(adminUserId);
        PolicyKey policyKey = parsePolicyKey(fullPolicyKey);
        SystemPolicy policy = systemPolicyRepository
                .findByPolicyGroupAndPolicyKey(policyKey.group(), policyKey.key())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "정책을 찾을 수 없습니다."));

        String value = request.value().trim();
        validateValue(policy.getValueType(), value);
        String beforeValue = serializePolicy(policy);
        policy.updateValue(value, admin);
        String afterValue = serializePolicy(policy);
        savePolicyUpdateLog(admin, policy, beforeValue, afterValue, request.memo());

        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public AdminRegionPolicyResponse getRegionPolicy(Long adminUserId) {
        ensureAdmin(adminUserId);
        return new AdminRegionPolicyResponse(
                policyService.getRequiredInteger(REGION_GROUP, REGION_CHANGE_COOLDOWN_KEY),
                policyService.getRequiredString(REGION_GROUP, REGION_REGISTRATION_SCOPE_KEY)
        );
    }

    @Transactional
    public AdminRegionPolicyResponse updateRegionPolicy(Long adminUserId, AdminRegionPolicyRequest request) {
        User admin = ensureSuperAdmin(adminUserId);
        updatePolicyValue(
                admin,
                REGION_GROUP,
                REGION_CHANGE_COOLDOWN_KEY,
                String.valueOf(request.regionChangeCooldownDays()),
                "지역 정책 변경"
        );
        updatePolicyValue(
                admin,
                REGION_GROUP,
                REGION_REGISTRATION_SCOPE_KEY,
                request.registrationScope().trim().toUpperCase(),
                "지역 정책 변경"
        );
        return new AdminRegionPolicyResponse(
                policyService.getRequiredInteger(REGION_GROUP, REGION_CHANGE_COOLDOWN_KEY),
                policyService.getRequiredString(REGION_GROUP, REGION_REGISTRATION_SCOPE_KEY)
        );
    }

    private void updatePolicyValue(User admin, String group, String key, String value, String memo) {
        SystemPolicy policy = systemPolicyRepository.findByPolicyGroupAndPolicyKey(group, key)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "정책을 찾을 수 없습니다."));
        validateValue(policy.getValueType(), value);
        String beforeValue = serializePolicy(policy);
        policy.updateValue(value, admin);
        String afterValue = serializePolicy(policy);
        savePolicyUpdateLog(admin, policy, beforeValue, afterValue, memo);
    }

    private User ensureAdmin(Long userId) {
        User user = getActiveUser(userId);
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
        return user;
    }

    private User ensureSuperAdmin(Long userId) {
        User user = getActiveUser(userId);
        if (user.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN, "최고 관리자 권한이 필요합니다.");
        }
        return user;
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return user;
    }

    private PolicyKey parsePolicyKey(String fullPolicyKey) {
        int separator = fullPolicyKey == null ? -1 : fullPolicyKey.indexOf('.');
        if (separator <= 0 || separator == fullPolicyKey.length() - 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 키 형식이 올바르지 않습니다.");
        }
        return new PolicyKey(fullPolicyKey.substring(0, separator), fullPolicyKey.substring(separator + 1));
    }

    private void validateValue(PolicyValueType valueType, String value) {
        switch (valueType) {
            case INTEGER -> validateInteger(value);
            case DECIMAL -> validateDecimal(value);
            case BOOLEAN -> validateBoolean(value);
            case STRING -> {
                if (value.isBlank()) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 값은 비어 있을 수 없습니다.");
                }
            }
        }
    }

    private void validateInteger(String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 값은 정수여야 합니다.");
        }
    }

    private void validateDecimal(String value) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 값은 숫자여야 합니다.");
        }
    }

    private void validateBoolean(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 값은 true 또는 false여야 합니다.");
        }
    }

    private void savePolicyUpdateLog(
            User admin,
            SystemPolicy policy,
            String beforeValue,
            String afterValue,
            String memo
    ) {
        adminActionLogRepository.save(new AdminActionLog(
                admin,
                POLICY_UPDATE_ACTION,
                POLICY_TARGET_TYPE,
                policy.getId(),
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String serializePolicy(SystemPolicy policy) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "policyGroup", policy.getPolicyGroup(),
                    "policyKey", policy.getPolicyKey(),
                    "policyValue", policy.getPolicyValue(),
                    "valueType", policy.getValueType().name()
            ));
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정책 변경 로그를 생성할 수 없습니다.");
        }
    }

    private AdminPolicyResponse toResponse(SystemPolicy policy) {
        User updatedBy = policy.getUpdatedBy();
        return new AdminPolicyResponse(
                policy.getId(),
                policy.getPolicyGroup(),
                policy.getPolicyKey(),
                policy.getPolicyGroup() + "." + policy.getPolicyKey(),
                policy.getPolicyValue(),
                policy.getValueType(),
                policy.getDescription(),
                policy.isActive(),
                updatedBy == null ? null : updatedBy.getId(),
                policy.getUpdatedAt()
        );
    }

    private record PolicyKey(String group, String key) {
    }
}
