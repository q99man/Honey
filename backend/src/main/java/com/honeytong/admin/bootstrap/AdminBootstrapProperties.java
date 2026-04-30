package com.honeytong.admin.bootstrap;

import com.honeytong.user.entity.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        String email,
        String password,
        String nickname,
        UserRole role,
        String phone,
        boolean phoneVerified,
        boolean resetPassword
) {
    public AdminBootstrapProperties {
        if (role == null) {
            role = UserRole.SUPER_ADMIN;
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = "local-admin";
        }
    }
}
