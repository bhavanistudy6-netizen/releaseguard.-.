package com.releaseguard.security;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        Set<Role> roles,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoles(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

