package com.honeytong.admin.bootstrap;

import com.honeytong.user.entity.UserRole;

public record AdminBootstrapResult(
        Long userId,
        String email,
        UserRole role,
        boolean created,
        boolean passwordUpdated
) {
}
