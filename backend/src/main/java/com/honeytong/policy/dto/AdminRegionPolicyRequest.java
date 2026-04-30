package com.honeytong.policy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRegionPolicyRequest(
        @NotNull
        @Min(0)
        Integer regionChangeCooldownDays,

        @NotBlank
        @Size(max = 30)
        String registrationScope
) {
}
