package com.honeytong.auth.service;

import com.honeytong.auth.dto.LoginRequest;
import com.honeytong.auth.dto.RefreshTokenRequest;
import com.honeytong.auth.dto.SignupRequest;
import com.honeytong.auth.dto.SignupResponse;
import com.honeytong.auth.dto.TokenResponse;
import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import com.honeytong.auth.repository.UserAuthRepository;
import com.honeytong.auth.security.JwtTokenProvider;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.user.entity.User;
import com.honeytong.user.entity.UserLevel;
import com.honeytong.user.entity.UserTrust;
import com.honeytong.user.repository.UserLevelRepository;
import com.honeytong.user.repository.UserRepository;
import com.honeytong.user.repository.UserTrustRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserLevelRepository userLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            UserTrustRepository userTrustRepository,
            UserLevelRepository userLevelRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.userTrustRepository = userTrustRepository;
        this.userLevelRepository = userLevelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userAuthRepository.existsByProviderAndProviderUserId(AuthProvider.LOCAL, request.email())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
        }

        User user = userRepository.save(new User(request.nickname(), request.email()));
        String passwordHash = passwordEncoder.encode(request.password());
        userAuthRepository.save(new UserAuth(user, AuthProvider.LOCAL, request.email(), request.email(), passwordHash));
        userTrustRepository.save(new UserTrust(user));
        userLevelRepository.save(new UserLevel(user));

        return new SignupResponse(user.getId(), user.getNickname());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userAuth.getUser();
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }

        userAuth.markLoggedIn();
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.consumeForRotation(request.refreshToken());
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이용할 수 없는 계정입니다.");
        }
        return issueTokenPair(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private TokenResponse issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new TokenResponse(accessToken, refreshToken);
    }
}
