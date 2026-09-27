package yadi.samuraiai.foundation.audit;

import java.util.*;

public final class AuditRegistry {
    private final Map<String, AuditCheck> checks = new LinkedHashMap<>();
    public synchronized void register(AuditCheck check) {
        Objects.requireNonNull(check);
        if (check.id() == null || !check.id().matches("[a-z0-9][a-z0-9._-]+") ||
                check.stage() == null || check.module() == null || check.module().isBlank() || check.failureSeverity() == null)
            throw new IllegalArgumentException("Auditor metadata incomplete");
        if (checks.putIfAbsent(check.id(), check) != null)
            throw new IllegalArgumentException("Duplicate auditor: " + check.id());
    }
    public synchronized List<AuditCheck> snapshot() {
        return checks.values().stream().sorted(Comparator.comparing(AuditCheck::stage).thenComparing(AuditCheck::id)).toList();
    }
    public static AuditRegistry discover(ClassLoader loader) {
        AuditRegistry registry = new AuditRegistry();
        ServiceLoader.load(AuditCheck.class, loader).forEach(registry::register);
        return registry;
    }
}
