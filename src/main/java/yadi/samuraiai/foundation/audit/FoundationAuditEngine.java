package yadi.samuraiai.foundation.audit;

import java.util.List;
import java.util.ServiceConfigurationError;

public final class FoundationAuditEngine {
    public AuditSummary run(AuditContext context) {
        long start = System.nanoTime();
        try {
            return AuditSummary.startup(context, new AuditPipeline().execute(
                    AuditRegistry.discover(context.resources()), context), System.nanoTime() - start);
        } catch (RuntimeException | LinkageError | ServiceConfigurationError error) {
            return AuditSummary.startup(context, List.of(new AuditResult("registry.discovery", AuditStage.STRUCTURE,
                    "core", AuditSeverity.BLOCKER, AuditOutcome.FAIL,
                    "Auditor discovery failed: " + error.getClass().getSimpleName(),
                    "Restore valid, unique audit service registrations.", 0)), System.nanoTime() - start);
        }
    }
}
