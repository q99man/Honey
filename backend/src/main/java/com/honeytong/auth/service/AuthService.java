package com.honeytong.auth.service;

import com.honeytong.auth.dto.LoginRequest;
import com.honeytong.auth.dto.OAuthLoginRequest;
import com.honeytong.auth.dto.RefreshTokenRequest;
import com.honeytong.auth.dto.SignupRequest;
import com.honeytong.auth.dto.SignupResponse;
import com.honeytong.auth.dto.TokenResponse;
import com.honeytong.auth.entity.AuthProvider;
import com.honeytong.auth.entity.UserAuth;
import com.honeytong.auth.oauth.OAuthProviderClientRegistry;
import com.honeytong.auth.oauth.OAuthUserProfile;
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
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserTrustRepository userTrustRepository;
    private final UserLevelRepository userLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuthProviderClientRegistry oauthProviderClientRegistry;

    public AuthService(
            UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            UserTrustRepository userTrustRepository,
            UserLevelRepository userLevelRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            OAuthProviderClientRegistry oauthProviderClientRegistry
    ) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.userTrustRepository = userTrustRepository;
        this.userLevelRepository = userLevelRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.oauthProviderClientRegistry = oauthProviderClientRegistry;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userAuthRepository.existsByProviderAndProviderUserId(AuthProvider.LOCAL, request.email())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
        }

        User user = userRepository.save(new User(request.nickname(), request.email()));
        String passwordHash = passwordEncoder.encode(request.password());
        userAuthRepository.save(new UserAuth(user, AuthProvider.LOCAL, request.email(), request.email(), passwordHash));
        initializeTrustAndLevel(user);

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
    public TokenResponse oauthLogin(String providerValue, OAuthLoginRequest request) {
        AuthProvider provider = parseOAuthProvider(providerValue);
        OAuthUserProfile profile = oauthProviderClientRegistry.fetchUserProfile(provider, request.accessToken());
        validateProviderProfile(profile);

        UserAuth userAuth = userAuthRepository.findByProviderAndProviderUserId(provider, profile.providerUserId())
                .orElseGet(() -> createOAuthUserAuth(provider, profile));

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

    private UserAuth createOAuthUserAuth(AuthProvider provider, OAuthUserProfile profile) {
        User user = userRepository.save(new User(resolveNickname(provider, profile), trimToNull(profile.email())));
        UserAuth userAuth = userAuthRepository.save(new UserAuth(
                user,
                provider,
                profile.providerUserId(),
                trimToNull(profile.email()),
                null
        ));
        initializeTrustAndLevel(user);
        return userAuth;
    }

    private void initializeTrustAndLevel(User user) {
        userTrustRepository.save(new UserTrust(user));
        userLevelRepository.save(new UserLevel(user));
    }

    private AuthProvider parseOAuthProvider(String providerValue) {
        try {
            AuthProvider provider = AuthProvider.valueOf(providerValue.toUpperCase(Locale.ROOT));
            if (provider == AuthProvider.LOCAL) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "LOCAL is not an OAuth provider.");
            }
            return provider;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported OAuth provider.");
        }
    }

    private void validateProviderProfile(OAuthUserProfile profile) {
        if (!StringUtils.hasText(profile.providerUserId()) || profile.providerUserId().length() > 200) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "OAuth provider user id is invalid.");
        }
    }

    private String resolveNickname(AuthProvider provider, OAuthUserProfile profile) {
        String nickname = trimToNull(profile.nickname());
        if (nickname == null) {
            nickname = provider.name().toLowerCase(Locale.ROOT) + "_user";
        }
        return nickname.length() > 50 ? nickname.substring(0, 50) : nickname;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
