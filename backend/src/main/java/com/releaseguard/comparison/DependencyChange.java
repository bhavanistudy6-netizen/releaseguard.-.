package com.releaseguard.comparison;

import java.util.List;

public record DependencyChange(
        String identity,
        DependencyChangeType changeType,
        List<DependencySnapshot> before,
        List<DependencySnapshot> after,
        String reason
) {
    public DependencyChange {
        before = List.copyOf(before);
        after = List.copyOf(after);
    }
}