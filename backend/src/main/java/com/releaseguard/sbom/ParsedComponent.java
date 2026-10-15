package com.releaseguard.sbom;

public record ParsedComponent(
        String bomRef,
        String componentType,
        String groupName,
        String name,
        String version,
        String purl) {
}