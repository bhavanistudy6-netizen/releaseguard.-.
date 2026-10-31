package com.releaseguard.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateUserRequest(
        @NotBlank
        @Size(max = 150)
        String displayName,

        @NotEmpty(message = "At least one role is required")
        Set<Role> roles,

        boolean enabled
) {}

