package com.releaseguard.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 1, max = 255) String password
) {}

