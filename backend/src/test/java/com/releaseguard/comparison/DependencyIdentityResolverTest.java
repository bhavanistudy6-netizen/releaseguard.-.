package com.releaseguard.comparison;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyIdentityResolverTest {

    private final DependencyIdentityResolver resolver =
            new DependencyIdentityResolver();

    @Test
    void matchesSamePackageAcrossVersions() {
        DependencyIdentity oldIdentity = resolver.resolve(snapshot(
                "3.17.0",
                "pkg:maven/org.apache.commons/commons-lang3@3.17.0"
        ));

        DependencyIdentity newIdentity = resolver.resolve(snapshot(
                "3.18.0",
                "pkg:maven/org.apache.commons/commons-lang3@3.18.0"
        ));

        assertTrue(oldIdentity.resolved());
        assertTrue(newIdentity.resolved());
        assertEquals(oldIdentity.key(), newIdentity.key());
        assertEquals(
                "pkg:maven/org.apache.commons/commons-lang3",
                oldIdentity.key()
        );
        assertEquals("3.17.0", oldIdentity.version());
        assertEquals("3.18.0", newIdentity.version());
    }

    @Test
    void retainsQualifiersInIdentity() {
        DependencyIdentity mainJar = resolver.resolve(snapshot(
                "1.0.0",
                "pkg:maven/org.example/demo@1.0.0?classifier=main"
        ));

        DependencyIdentity sourcesJar = resolver.resolve(snapshot(
                "1.0.0",
                "pkg:maven/org.example/demo@1.0.0?classifier=sources"
        ));

        assertTrue(mainJar.resolved());
        assertTrue(sourcesJar.resolved());
        assertNotEquals(mainJar.key(), sourcesJar.key());
    }

    @Test
    void retainsSubpathInIdentity() {
        DependencyIdentity first = resolver.resolve(snapshot(
                "1.0.0",
                "pkg:generic/demo@1.0.0#first"
        ));

        DependencyIdentity second = resolver.resolve(snapshot(
                "1.0.0",
                "pkg:generic/demo@1.0.0#second"
        ));

        assertTrue(first.resolved());
        assertTrue(second.resolved());
        assertNotEquals(first.key(), second.key());
    }

    @Test
    void usesPackageUrlVersionWhenComponentVersionIsMissing() {
        DependencyIdentity identity = resolver.resolve(snapshot(
                null,
                "pkg:maven/org.example/demo@1.0.0"
        ));

        assertTrue(identity.resolved());
        assertEquals("1.0.0", identity.version());
    }

    @Test
    void allowsIdentityWithUnknownVersion() {
        DependencyIdentity identity = resolver.resolve(snapshot(
                null,
                "pkg:maven/org.example/demo"
        ));

        assertTrue(identity.resolved());
        assertNull(identity.version());
    }

    @Test
    void reportsConflictingVersions() {
        DependencyIdentity identity = resolver.resolve(snapshot(
                "2.0.0",
                "pkg:maven/org.example/demo@1.0.0"
        ));

        assertFalse(identity.resolved());
        assertEquals(
                "Component version conflicts with package URL version",
                identity.issue()
        );
    }

    @Test
    void reportsMissingPackageUrl() {
        DependencyIdentity identity = resolver.resolve(snapshot(
                "1.0.0",
                null
        ));

        assertFalse(identity.resolved());
        assertEquals("Package URL is missing", identity.issue());
    }

    @Test
    void reportsInvalidPackageUrl() {
        DependencyIdentity identity = resolver.resolve(snapshot(
                "1.0.0",
                "not-a-package-url"
        ));

        assertFalse(identity.resolved());
        assertEquals("Package URL is invalid", identity.issue());
    }

    private DependencySnapshot snapshot(String version, String purl) {
        return new DependencySnapshot(
                1L,
                "library",
                "org.example",
                "example",
                version,
                purl
        );
    }
}