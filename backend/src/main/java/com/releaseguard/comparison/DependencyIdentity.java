package com.releaseguard.comparison;

public record DependencyIdentity(
        String key,
        String version,
        String issue
) {
    public boolean resolved() {
        return key != null;
    }
}