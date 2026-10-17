package com.releaseguard.sbom;

import java.util.List;

public record ParsedSbom(
        String bomFormat,
        String specVersion,
        List<ParsedComponent> components) {
    public ParsedSbom {
        components = List.copyOf(components);
    }
}
