package com.releaseguard.release;

import java.util.List;

public record ReleasePageResponse(
        List<ReleaseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}