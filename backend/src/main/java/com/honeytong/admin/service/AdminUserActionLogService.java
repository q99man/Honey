package com.honeytong.admin.service;

import com.honeytong.admin.dto.AdminUserActionLogResponse;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserActionLog;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserActionLogRepository;
import com.honeytong.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserActionLogService {

    private final UserActionLogRepository userActionLogRepository;
    private final UserRepository userRepository;

    public AdminUserActionLogService(
            UserActionLogRepository userActionLogRepository,
            UserRepository userRepository
    ) {
        this.userActionLogRepository = userActionLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserActionLogResponse> getUserActionLogs(Long adminUserId) {
        ensureAdmin(adminUserId);
        return userActionLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private User ensureAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive() || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Admin permission is required.");
        }
        return user;
    }

    private AdminUserActionLogResponse toResponse(UserActionLog log) {
        User user = log.getUser();
        return new AdminUserActionLogResponse(
                log.getId(),
                user.getId(),
                user.getNickname(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getMetadataJson(),
                log.getCreatedAt()
        );
    }
}
