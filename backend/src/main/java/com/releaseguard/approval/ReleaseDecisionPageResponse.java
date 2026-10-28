package com.releaseguard.approval;

import java.util.List;

public record ReleaseDecisionPageResponse(
        List<ReleaseDecisionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}