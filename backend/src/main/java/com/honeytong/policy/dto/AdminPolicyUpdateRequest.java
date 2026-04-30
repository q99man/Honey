package com.honeytong.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPolicyUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String value,

        @Size(max = 255)
        String memo
) {
}
