package com.releaseguard.comparison;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
public class DependencyComparisonEngine {

    private final DependencyIdentityResolver resolver;

    public DependencyComparisonEngine(DependencyIdentityResolver resolver) {
        this.resolver = resolver;
    }

    public List<DependencyChange> compare(
            List<DependencySnapshot> before,
            List<DependencySnapshot> after) {

        List<DependencyChange> results = new ArrayList<>();

        Map<String, List<ResolvedDependency>> oldInventory = index(before, true, results);

        Map<String, List<ResolvedDependency>> newInventory = index(after, false, results);

        TreeSet<String> identities = new TreeSet<>();
        identities.addAll(oldInventory.keySet());
        identities.addAll(newInventory.keySet());

        for (String identity : identities) {
            List<ResolvedDependency> oldEntries = oldInventory.getOrDefault(identity, List.of());

            List<ResolvedDependency> newEntries = newInventory.getOrDefault(identity, List.of());

            DependencyChangeType type;
            String reason = null;

            if (oldEntries.size() > 1 || newEntries.size() > 1) {
                type = DependencyChangeType.UNCERTAIN;
                reason = "Multiple components share this package identity";
            } else if (oldEntries.isEmpty()) {
                type = DependencyChangeType.ADDED;
            } else if (newEntries.isEmpty()) {
                type = DependencyChangeType.REMOVED;
            } else {
                String oldVersion = oldEntries.getFirst().identity().version();

                String newVersion = newEntries.getFirst().identity().version();

                if (!hasText(oldVersion) || !hasText(newVersion)) {
                    type = DependencyChangeType.UNCERTAIN;
                    reason = "A component version is missing";
                } else if (oldVersion.equals(newVersion)) {
                    type = DependencyChangeType.UNCHANGED;
                } else {
                    type = DependencyChangeType.VERSION_CHANGED;
                }
            }

            results.add(new DependencyChange(
                    identity,
                    type,
                    snapshots(oldEntries),
                    snapshots(newEntries),
                    reason));
        }

        return List.copyOf(results);
    }

    private Map<String, List<ResolvedDependency>> index(
            List<DependencySnapshot> components,
            boolean oldInventory,
            List<DependencyChange> results) {

        Map<String, List<ResolvedDependency>> indexed = new TreeMap<>();

        for (DependencySnapshot component : components) {
            DependencyIdentity identity = resolver.resolve(component);

            if (!identity.resolved()) {
                results.add(new DependencyChange(
                        null,
                        DependencyChangeType.UNCERTAIN,
                        oldInventory ? List.of(component) : List.of(),
                        oldInventory ? List.of() : List.of(component),
                        identity.issue()));

                continue;
            }

            indexed.computeIfAbsent(
                    identity.key(),
                    ignored -> new ArrayList<>()).add(new ResolvedDependency(component, identity));
        }

        return indexed;
    }

    private List<DependencySnapshot> snapshots(
            List<ResolvedDependency> entries) {

        return entries.stream()
                .map(ResolvedDependency::snapshot)
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ResolvedDependency(
            DependencySnapshot snapshot,
            DependencyIdentity identity) {
    }
}