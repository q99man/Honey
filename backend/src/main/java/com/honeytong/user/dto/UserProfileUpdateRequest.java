package com.honeytong.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "언어 설정은 필수입니다.")
        @Pattern(regexp = "^(ko|en|ja)$", message = "지원하지 않는 언어 설정입니다.")
        String languagePreference
) {
}
