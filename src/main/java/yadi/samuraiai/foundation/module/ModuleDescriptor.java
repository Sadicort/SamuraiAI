package yadi.samuraiai.foundation.module;

import java.util.*;

public record ModuleDescriptor(String id, String name, String version, String author,
                               List<ModuleDependency> dependencies, ModuleSide side, int priority) {
    public ModuleDescriptor {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(version);
        Objects.requireNonNull(author); Objects.requireNonNull(side);
        if (!id.matches("[a-z][a-z0-9_.-]*")) throw new IllegalArgumentException("Invalid module id: " + id);
        if (name.isBlank() || version.isBlank() || author.isBlank()) throw new IllegalArgumentException("Blank module metadata: " + id);
        dependencies = List.copyOf(Objects.requireNonNull(dependencies));
        if (dependencies.stream().map(ModuleDependency::id).distinct().count() != dependencies.size())
            throw new IllegalArgumentException("Duplicate dependency in " + id);
        if (dependencies.stream().anyMatch(d -> d.id().equals(id))) throw new IllegalArgumentException("Module depends on itself: " + id);
    }
}
