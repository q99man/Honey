package com.honeytong.admin.service;

import com.honeytong.admin.dto.AdminActionLogResponse;
import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActionLogService {

    private final AdminActionLogRepository adminActionLogRepository;
    private final UserRepository userRepository;

    public AdminActionLogService(
            AdminActionLogRepository adminActionLogRepository,
            UserRepository userRepository
    ) {
        this.adminActionLogRepository = adminActionLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminActionLogResponse> getActionLogs(Long adminUserId) {
        ensureAdmin(adminUserId);
        return adminActionLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
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

    private AdminActionLogResponse toResponse(AdminActionLog log) {
        User admin = log.getAdminUser();
        return new AdminActionLogResponse(
                log.getId(),
                admin.getId(),
                admin.getNickname(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getMemo(),
                log.getCreatedAt()
        );
    }
}
