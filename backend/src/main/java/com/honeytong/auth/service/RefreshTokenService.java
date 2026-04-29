package com.honeytong.auth.service;

import com.honeytong.auth.config.SecurityProperties;
import com.honeytong.auth.entity.RefreshToken;
import com.honeytong.auth.repository.RefreshTokenRepository;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecurityProperties securityProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(securityProperties.refreshTokenDays());
        refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt));
        return rawToken;
    }

    @Transactional
    public User consumeForRotation(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."));

        if (refreshToken.isExpired()) {
            refreshToken.revoke();
            throw new ApiException(ErrorCode.UNAUTHORIZED, "만료된 리프레시 토큰입니다.");
        }

        refreshToken.revoke();
        return refreshToken.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
        }
    }
}
