package com.releaseguard;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @GetMapping("/api/v1/info")
    public Map<String, String> getInfo() {
        return Map.of(
            "name", "ReleaseGuard",
            "description", "Dependency Risk and Release Traceability Platform"
        );
    }
}