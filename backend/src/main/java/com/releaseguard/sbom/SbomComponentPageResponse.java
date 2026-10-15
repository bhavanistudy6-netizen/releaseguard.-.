package com.releaseguard.sbom;

import java.util.List;

public record SbomComponentPageResponse(
        List<SbomComponentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}