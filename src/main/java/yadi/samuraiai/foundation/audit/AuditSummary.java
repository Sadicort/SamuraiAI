package yadi.samuraiai.foundation.audit;

import java.util.*;

/** Startup audit is not release certification, even when every startup check passes. */
public record AuditSummary(String runId, String timestamp, String profile, String foundation,
                           boolean startupAllowed, boolean phase2Unlocked, List<String> disabledModules,
                           Map<String, String> versions, Map<String, String> environment,
                           List<AuditResult> results, long durationNanos) {
    public AuditSummary {
        disabledModules = List.copyOf(disabledModules);
        versions = Map.copyOf(versions); environment = Map.copyOf(environment);
        results = List.copyOf(results);
    }
    public static AuditSummary startup(AuditContext context, List<AuditResult> results, long nanos) {
        boolean blocked = results.stream().anyMatch(r -> r.failed() && r.severity() == AuditSeverity.BLOCKER);
        boolean failed = results.stream().anyMatch(r -> r.failed() && r.severity().ordinal() >= AuditSeverity.ERROR.ordinal());
        List<String> disabled = results.stream().filter(r -> r.failed() && r.severity() == AuditSeverity.CRITICAL)
                .map(AuditResult::module).distinct().sorted().toList();
        return new AuditSummary(UUID.randomUUID().toString(), java.time.Instant.now().toString(),
                context.side().name(), blocked ? "BLOCKED" : failed ? "FAILED" : "PARTIAL",
                !blocked, false, disabled, context.versions(), context.environment(), results, Math.max(0, nanos));
    }
}
