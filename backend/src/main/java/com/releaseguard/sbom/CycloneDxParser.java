package com.releaseguard.sbom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CycloneDxParser {

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_COMPONENTS = 10_000;
    private static final int MAX_COMPONENT_DEPTH = 32;

    private final ObjectMapper objectMapper;

    public CycloneDxParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedSbom parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw invalid("SBOM file must not be empty");
        }

        if (content.length > MAX_BYTES) {
            throw invalid("SBOM file must not exceed 10 MiB");
        }

        JsonNode root;

        try {
            root = objectMapper.readTree(content);
        } catch (JacksonException exception) {
            throw invalid("SBOM file must contain valid JSON");
        }

        if (root == null || !root.isObject()) {
            throw invalid("SBOM must be a JSON object");
        }

        String format = requiredText(root, "bomFormat", 30);
        String specVersion = requiredText(root, "specVersion", 20);

        if (!"CycloneDX".equals(format)) {
            throw invalid("Only CycloneDX SBOMs are supported");
        }

        if (!"1.6".equals(specVersion)) {
            throw invalid("Only CycloneDX version 1.6 is supported");
        }

        JsonNode componentsNode = root.get("components");

        if (componentsNode == null || !componentsNode.isArray()) {
            throw invalid("SBOM must contain a components array");
        }

        List<ParsedComponent> components = new ArrayList<>();
        Set<String> references = new HashSet<>();

        collectComponents(
                componentsNode,
                components,
                references,
                1);

        return new ParsedSbom(format, specVersion, components);
    }

    private void collectComponents(
            JsonNode nodes,
            List<ParsedComponent> components,
            Set<String> references,
            int depth) {

        if (depth > MAX_COMPONENT_DEPTH) {
            throw invalid("Component nesting must not exceed 32 levels");
        }

        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                throw invalid("Each component must be a JSON object");
            }

            if (components.size() >= MAX_COMPONENTS) {
                throw invalid("SBOM must not exceed 10000 components");
            }

            String bomRef = optionalText(node, "bom-ref", 255);

            if (bomRef != null && !references.add(bomRef)) {
                throw invalid("Duplicate component bom-ref: " + bomRef);
            }

            components.add(new ParsedComponent(
                    bomRef,
                    requiredText(node, "type", 50),
                    optionalText(node, "group", 255),
                    requiredText(node, "name", 255),
                    optionalText(node, "version", 255),
                    optionalText(node, "purl", 4096)));

            JsonNode nested = node.get("components");

            if (nested != null) {
                if (!nested.isArray()) {
                    throw invalid(
                            "Nested components must be a JSON array");
                }

                collectComponents(
                        nested,
                        components,
                        references,
                        depth + 1);
            }
        }
    }

    private String requiredText(
            JsonNode node,
            String field,
            int maxLength) {

        String value = optionalText(node, field, maxLength);

        if (value == null) {
            throw invalid("Missing required field: " + field);
        }

        return value;
    }

    private String optionalText(
            JsonNode node,
            String field,
            int maxLength) {

        JsonNode value = node.get(field);

        if (value == null) {
            return null;
        }

        if (!value.isString()) {
            throw invalid(field + " must be a string");
        }

        String text = value.stringValue();

        if (text.isBlank()) {
            throw invalid(field + " must not be blank");
        }

        if (text.length() > maxLength) {
            throw invalid(
                    field + " must not exceed "
                            + maxLength + " characters");
        }

        if (text.indexOf('\0') >= 0) {
            throw invalid(field + " must not contain a null character");
        }

        return text;
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }
}