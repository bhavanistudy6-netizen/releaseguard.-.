package com.releaseguard.security;

public enum Role {
    VIEWER,
    DEVELOPER,
    REVIEWER,
    DEPLOYER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}

