package com.releaseguard.comparison;

import com.releaseguard.sbom.SbomComponent;

public record DependencySnapshot(
        Long componentId,
        String componentType,
        String groupName,
        String name,
        String version,
        String purl) {
    public static DependencySnapshot from(SbomComponent component) {
        return new DependencySnapshot(
                component.getId(),
                component.getComponentType(),
                component.getGroupName(),
                component.getName(),
                component.getVersion(),
                component.getPurl());
    }
}