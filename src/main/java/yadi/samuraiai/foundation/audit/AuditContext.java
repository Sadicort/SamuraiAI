package yadi.samuraiai.foundation.audit;

import java.util.*;

/** Immutable startup snapshot; no Minecraft entities, mutable configuration or optional class references. */
public record AuditContext(Side side, Map<String, String> versions,
                           Map<String, String> environment, ClassLoader resources) {
    public enum Side { CLIENT, DEDICATED_SERVER }
    public AuditContext {
        Objects.requireNonNull(side);
        versions = Map.copyOf(versions);
        environment = Map.copyOf(environment);
        Objects.requireNonNull(resources);
    }
}
