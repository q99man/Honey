package com.honeytong.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationVerifyRequest(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @NotBlank(message = "인증 코드는 필수입니다.")
        @Pattern(regexp = "^[0-9]{4,10}$", message = "인증 코드 형식이 올바르지 않습니다.")
        String code
) {
}
