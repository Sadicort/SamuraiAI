package yadi.samuraiai.foundation.audit;

import java.util.*;

public final class AuditPipeline {
    public List<AuditResult> execute(AuditRegistry registry, AuditContext context) {
        List<AuditResult> results = new ArrayList<>();
        for (AuditCheck check : registry.snapshot()) {
            long started = System.nanoTime();
            AuditResult result;
            try {
                result = Objects.requireNonNull(check.evaluate(context), "Auditor returned null");
                if (!result.id().equals(check.id()) || result.stage() != check.stage() ||
                        !result.module().equals(check.module()) || result.severity() != check.failureSeverity())
                    throw new IllegalStateException("Auditor returned mismatched metadata");
            } catch (Exception | LinkageError failure) {
                result = check.result(AuditOutcome.FAIL, "Auditor failed: " + failure.getClass().getSimpleName(),
                        "Inspect auditor " + check.id() + " and rerun; failure is not a pass.");
            }
            results.add(result.timed(System.nanoTime() - started));
        }
        // Empty stages remain explicitly unverified, never green through absence of auditors.
        for (AuditStage stage : AuditStage.values()) {
            if (results.stream().noneMatch(result -> result.stage() == stage))
                results.add(new AuditResult("coverage." + stage.name().toLowerCase(Locale.ROOT), stage,
                        "foundation", AuditSeverity.WARNING, AuditOutcome.NOT_RUN,
                        "No auditor registered for this stage", "Implement and register this stage's auditors.", 0));
        }
        if (registry.snapshot().isEmpty()) results.add(new AuditResult("registry.empty", AuditStage.STRUCTURE,
                "core", AuditSeverity.BLOCKER, AuditOutcome.FAIL, "No startup auditors discovered",
                "Restore META-INF/services and audit implementations.", 0));
        results.sort(Comparator.comparing(AuditResult::stage).thenComparing(AuditResult::id));
        return List.copyOf(results);
    }
}
