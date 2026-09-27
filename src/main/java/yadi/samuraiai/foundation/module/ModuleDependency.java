package yadi.samuraiai.foundation.module;

import java.util.Objects;

public record ModuleDependency(String id, boolean required) {
    public ModuleDependency {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z][a-z0-9_.-]*")) throw new IllegalArgumentException("Invalid module dependency: " + id);
    }
    public static ModuleDependency required(String id) { return new ModuleDependency(id, true); }
    public static ModuleDependency optional(String id) { return new ModuleDependency(id, false); }
}
