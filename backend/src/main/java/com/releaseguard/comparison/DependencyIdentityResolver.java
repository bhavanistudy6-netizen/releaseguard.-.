package com.releaseguard.comparison;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Component
public class DependencyIdentityResolver {

    public DependencyIdentity resolve(DependencySnapshot component) {
        String purlText = component.purl();

        if (purlText == null || purlText.isBlank()) {
            return unresolved("Package URL is missing");
        }

        try {
            PackageURL purl = new PackageURL(purlText);

            String declaredVersion = component.version();
            String purlVersion = purl.getVersion();

            if (hasText(declaredVersion)
                    && hasText(purlVersion)
                    && !declaredVersion.equals(purlVersion)) {
                return unresolved(
                        "Component version conflicts with package URL version");
            }

            TreeMap<String, String> qualifiers = purl.getQualifiers() == null
                    ? null
                    : new TreeMap<>(purl.getQualifiers());

            PackageURL identity = new PackageURL(
                    purl.getType(),
                    purl.getNamespace(),
                    purl.getName(),
                    null,
                    qualifiers,
                    purl.getSubpath());

            String version = hasText(declaredVersion)
                    ? declaredVersion
                    : purlVersion;

            return new DependencyIdentity(
                    identity.canonicalize(),
                    version,
                    null);
        } catch (MalformedPackageURLException exception) {
            return unresolved("Package URL is invalid");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private DependencyIdentity unresolved(String issue) {
        return new DependencyIdentity(null, null, issue);
    }
}