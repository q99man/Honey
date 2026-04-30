package com.honeytong.admin.bootstrap;

import com.honeytong.admin.entity.AdminActionLog;
import com.honeytong.admin.repository.AdminActionLogRepository;
import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import com.honeytong.auth.repository.UserAuthRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserRole;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private static final String ACTION_TYPE = "ADMIN_BOOTSTRAP";
    private static final String TARGET_TYPE = "USER";

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserLevelRepository userLevelRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            UserTrustRepository userTrustRepository,
            UserLevelRepository userLevelRepository,
            AdminActionLogRepository adminActionLogRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.userTrustRepository = userTrustRepository;
        this.userLevelRepository = userLevelRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminBootstrapResult bootstrap() {
        validateConfiguration();

        return userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, properties.email())
                .map(this::bootstrapExistingUser)
                .orElseGet(this::bootstrapNewUser);
    }

    private AdminBootstrapResult bootstrapExistingUser(UserAuth userAuth) {
        User user = userAuth.getUser();
        String beforeValue = describeUser(user, false);
        boolean passwordUpdated = false;

        user.promoteTo(properties.role());
        applyOptionalPhoneVerification(user);
        ensureUserStateRows(user);

        if (properties.resetPassword()) {
            userAuth.updatePasswordHash(passwordEncoder.encode(properties.password()));
            passwordUpdated = true;
        }

        saveLog(user, beforeValue, describeUser(user, passwordUpdated), "Existing local account bootstrapped");
        return new AdminBootstrapResult(user.getId(), properties.email(), user.getRole(), false, passwordUpdated);
    }

    private AdminBootstrapResult bootstrapNewUser() {
        User user = new User(properties.nickname(), properties.email());
        user.promoteTo(properties.role());
        applyOptionalPhoneVerification(user);
        userRepository.save(user);

        userAuthRepository.save(new UserAuth(
                user,
                AuthProvider.LOCAL,
                properties.email(),
                properties.email(),
                passwordEncoder.encode(properties.password())
        ));
        ensureUserStateRows(user);

        saveLog(user, null, describeUser(user, true), "New local admin account bootstrapped");
        return new AdminBootstrapResult(user.getId(), properties.email(), user.getRole(), true, true);
    }

    private void validateConfiguration() {
        if (isBlank(properties.email()) || isBlank(properties.password())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "관리자 부트스트랩 이메일과 비밀번호가 필요합니다.");
        }
        if (properties.role() != UserRole.ADMIN && properties.role() != UserRole.SUPER_ADMIN) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "관리자 부트스트랩 권한은 ADMIN 또는 SUPER_ADMIN이어야 합니다.");
        }
    }

    private void applyOptionalPhoneVerification(User user) {
        if (!isBlank(properties.phone()) && properties.phoneVerified()) {
            user.verifyPhone(properties.phone());
        }
    }

    private void ensureUserStateRows(User user) {
        if (!userTrustRepository.existsById(user.getId())) {
            UserTrust userTrust = new UserTrust(user);
            if (user.isPhoneVerified()) {
                userTrust.markPhoneVerified();
            }
            userTrustRepository.save(userTrust);
        }
        if (!userLevelRepository.existsById(user.getId())) {
            userLevelRepository.save(new UserLevel(user));
        }
    }

    private void saveLog(User adminUser, String beforeValue, String afterValue, String memo) {
        adminActionLogRepository.save(new AdminActionLog(
                adminUser,
                ACTION_TYPE,
                TARGET_TYPE,
                adminUser.getId(),
                beforeValue,
                afterValue,
                memo
        ));
    }

    private String describeUser(User user, boolean passwordUpdated) {
        return "{\"email\":\"" + properties.email()
                + "\",\"role\":\"" + user.getRole().name()
                + "\",\"phoneVerified\":" + user.isPhoneVerified()
                + ",\"passwordUpdated\":" + passwordUpdated
                + "}";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
