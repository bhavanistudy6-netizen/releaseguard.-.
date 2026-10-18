package com.releaseguard.comparison;

import java.util.List;

public record ReleaseComparisonResponse(
        Long applicationId,
        Long fromReleaseId,
        Long toReleaseId,
        List<DependencyChange> changes) {
    public ReleaseComparisonResponse {
        changes = List.copyOf(changes);
    }
}