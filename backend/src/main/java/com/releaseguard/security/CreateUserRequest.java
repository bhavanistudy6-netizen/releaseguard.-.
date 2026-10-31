package com.releaseguard.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain alphanumeric characters, dots, underscores, and hyphens")
        String username,

        @NotBlank
        @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
        String password,

        @NotBlank
        @Size(max = 150)
        String displayName,

        @NotEmpty(message = "At least one role is required")
        Set<Role> roles
) {}

