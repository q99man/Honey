package com.honeytong.auth.security;

import com.honeytong.auth.config.SecurityProperties;
import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import com.honeytong.policy.service.PolicyService;
import com.honeytong.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final SecurityProperties properties;
    private final PolicyService policyService;
    private final SecretKey secretKey;

    public JwtTokenProvider(SecurityProperties properties, PolicyService policyService) {
        this.properties = properties;
        this.policyService = policyService;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(getAccessTokenMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim("role", user.getRole().name())
                .claim("nickname", user.getNickname())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseAccessClaims(token);
        String role = claims.get("role", String.class);
        return new UsernamePasswordAuthenticationToken(
                Long.valueOf(claims.getSubject()),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    public boolean isValidAccessToken(String token) {
        try {
            parseAccessClaims(token);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private Claims parseAccessClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "액세스 토큰이 아닙니다.");
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }

    private int getAccessTokenMinutes() {
        int minutes = policyService.getRequiredInteger("auth", "jwt_access_token_minutes");
        if (minutes < 1 || minutes > 10080) { // 1분에서 7일(10080분) 사이
            throw new ApiException(ErrorCode.POLICY_VIOLATION, "액세스 토큰 만료 시간은 1분 이상 10080분(7일) 이하여야 합니다.");
        }
        return minutes;
    }
}
