package com.honeytong.auth.controller;

import com.honeytong.auth.dto.LoginRequest;
import com.honeytong.auth.dto.OAuthLoginRequest;
import com.honeytong.auth.dto.PhoneVerificationSendRequest;
import com.honeytong.auth.dto.PhoneVerificationSendResponse;
import com.honeytong.auth.dto.PhoneVerificationStatusResponse;
import com.honeytong.auth.dto.PhoneVerificationVerifyRequest;
import com.honeytong.auth.dto.RefreshTokenRequest;
import com.honeytong.auth.dto.SignupRequest;
import com.honeytong.auth.dto.SignupResponse;
import com.honeytong.auth.dto.TokenResponse;
import com.honeytong.auth.service.AuthService;
import com.honeytong.auth.service.PhoneVerificationService;
import com.honeytong.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    public AuthController(AuthService authService, PhoneVerificationService phoneVerificationService) {
        this.authService = authService;
        this.phoneVerificationService = phoneVerificationService;
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request), "Signup completed");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    @PostMapping("/oauth/{provider}")
    public ApiResponse<TokenResponse> oauthLogin(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        return ApiResponse.success(authService.oauthLogin(provider, request), "OAuth login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request), "Token refreshed");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(null, "Logout completed");
    }

    @PostMapping("/phone/send-code")
    public ApiResponse<PhoneVerificationSendResponse> sendPhoneVerificationCode(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PhoneVerificationSendRequest request
    ) {
        return ApiResponse.success(
                phoneVerificationService.sendCode(userId, request),
                "Verification code sent"
        );
    }

    @PostMapping("/phone/verify-code")
    public ApiResponse<PhoneVerificationStatusResponse> verifyPhoneCode(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PhoneVerificationVerifyRequest request
    ) {
        return ApiResponse.success(
                phoneVerificationService.verifyCode(userId, request),
                "Phone verification completed"
        );
    }

    @GetMapping("/phone/status")
    public ApiResponse<PhoneVerificationStatusResponse> phoneVerificationStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(phoneVerificationService.getStatus(userId), "OK");
    }
}
