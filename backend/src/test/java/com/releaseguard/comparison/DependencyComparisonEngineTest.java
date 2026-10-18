package com.releaseguard.comparison;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyComparisonEngineTest {

    private final DependencyComparisonEngine engine =
            new DependencyComparisonEngine(
                    new DependencyIdentityResolver()
            );

    @Test
    void identifiesAddedDependency() {
        DependencySnapshot dependency = dependency(1L, "demo", "1.0.0");

        var changes = engine.compare(List.of(), List.of(dependency));

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.ADDED,
                changes.getFirst().changeType()
        );
        assertTrue(changes.getFirst().before().isEmpty());
        assertEquals(List.of(dependency), changes.getFirst().after());
    }

    @Test
    void identifiesRemovedDependency() {
        DependencySnapshot dependency = dependency(1L, "demo", "1.0.0");

        var changes = engine.compare(List.of(dependency), List.of());

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.REMOVED,
                changes.getFirst().changeType()
        );
        assertEquals(List.of(dependency), changes.getFirst().before());
        assertTrue(changes.getFirst().after().isEmpty());
    }

    @Test
    void identifiesVersionChange() {
        DependencySnapshot oldDependency =
                dependency(1L, "demo", "1.0.0");

        DependencySnapshot newDependency =
                dependency(2L, "demo", "2.0.0");

        var changes = engine.compare(
                List.of(oldDependency),
                List.of(newDependency)
        );

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.VERSION_CHANGED,
                changes.getFirst().changeType()
        );
        assertEquals(
                "1.0.0",
                changes.getFirst().before().getFirst().version()
        );
        assertEquals(
                "2.0.0",
                changes.getFirst().after().getFirst().version()
        );
    }

    @Test
    void identifiesUnchangedVersionDespiteDifferentDatabaseIds() {
        var changes = engine.compare(
                List.of(dependency(1L, "demo", "1.0.0")),
                List.of(dependency(99L, "demo", "1.0.0"))
        );

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.UNCHANGED,
                changes.getFirst().changeType()
        );
    }

    @Test
    void reportsMissingVersionAsUncertain() {
        DependencySnapshot unknownVersion = new DependencySnapshot(
                1L,
                "library",
                "org.example",
                "demo",
                null,
                "pkg:maven/org.example/demo"
        );

        var changes = engine.compare(
                List.of(unknownVersion),
                List.of(dependency(2L, "demo", "2.0.0"))
        );

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.UNCERTAIN,
                changes.getFirst().changeType()
        );
        assertEquals(
                "A component version is missing",
                changes.getFirst().reason()
        );
    }

    @Test
    void preservesMultipleMatchingVersionsAsUncertain() {
        DependencySnapshot first = dependency(1L, "demo", "1.0.0");
        DependencySnapshot second = dependency(2L, "demo", "2.0.0");
        DependencySnapshot latest = dependency(3L, "demo", "3.0.0");

        var changes = engine.compare(
                List.of(first, second),
                List.of(latest)
        );

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.UNCERTAIN,
                changes.getFirst().changeType()
        );
        assertEquals(
                List.of(first, second),
                changes.getFirst().before()
        );
        assertEquals(List.of(latest), changes.getFirst().after());
    }

    @Test
    void doesNotGuessIdentityWhenPackageUrlIsMissing() {
        DependencySnapshot unidentified = new DependencySnapshot(
                1L,
                "library",
                "org.example",
                "demo",
                "1.0.0",
                null
        );

        var changes = engine.compare(
                List.of(unidentified),
                List.of()
        );

        assertEquals(1, changes.size());
        assertEquals(
                DependencyChangeType.UNCERTAIN,
                changes.getFirst().changeType()
        );
        assertNull(changes.getFirst().identity());
        assertEquals(
                List.of(unidentified),
                changes.getFirst().before()
        );
        assertEquals(
                "Package URL is missing",
                changes.getFirst().reason()
        );
    }

    @Test
    void keepsDifferentPackageQualifiersSeparate() {
        DependencySnapshot mainJar = new DependencySnapshot(
                1L,
                "library",
                "org.example",
                "demo",
                "1.0.0",
                "pkg:maven/org.example/demo@1.0.0?classifier=main"
        );

        DependencySnapshot sourcesJar = new DependencySnapshot(
                2L,
                "library",
                "org.example",
                "demo",
                "1.0.0",
                "pkg:maven/org.example/demo@1.0.0?classifier=sources"
        );

        var changes = engine.compare(
                List.of(mainJar),
                List.of(sourcesJar)
        );

        assertEquals(2, changes.size());

        assertEquals(
                1L,
                changes.stream()
                        .filter(change ->
                                change.changeType()
                                        == DependencyChangeType.ADDED)
                        .count()
        );

        assertEquals(
                1L,
                changes.stream()
                        .filter(change ->
                                change.changeType()
                                        == DependencyChangeType.REMOVED)
                        .count()
        );
    }

    @Test
    void returnsEmptyResultForEmptyInventories() {
        assertTrue(engine.compare(List.of(), List.of()).isEmpty());
    }

    private DependencySnapshot dependency(
            Long id,
            String name,
            String version) {

        return new DependencySnapshot(
                id,
                "library",
                "org.example",
                name,
                version,
                "pkg:maven/org.example/" + name + "@" + version
        );
    }
}