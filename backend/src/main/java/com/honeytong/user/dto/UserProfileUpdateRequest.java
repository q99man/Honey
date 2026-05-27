package com.honeytong.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UserProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "언어 설정은 필수입니다.")
        @Pattern(regexp = "^(ko|en|ja)$", message = "지원하지 않는 언어 설정입니다.")
        String languagePreference,

        @Min(value = 1900, message = "출생년도는 1900년 이후여야 합니다.")
        @Max(value = 2100, message = "출생년도가 올바르지 않습니다.")
        Integer birthYear,

        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다.")
        String gender,

        @Size(max = 10, message = "국가코드는 10자 이하여야 합니다.")
        String nationalityCode,

        @Size(max = 2048, message = "프로필 이미지 URL은 2048자 이하여야 합니다.")
        String profileImageUrl
) {
}
