package com.honeytong.auth.repository;

import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<UserAuth> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
