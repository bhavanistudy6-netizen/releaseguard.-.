package com.releaseguard.sbom;

public record SbomComponentResponse(
        Long id,
        String bomRef,
        String componentType,
        String groupName,
        String name,
        String version,
        String purl) {
    public static SbomComponentResponse from(SbomComponent component) {
        return new SbomComponentResponse(
                component.getId(),
                component.getBomRef(),
                component.getComponentType(),
                component.getGroupName(),
                component.getName(),
                component.getVersion(),
                component.getPurl());
    }
}