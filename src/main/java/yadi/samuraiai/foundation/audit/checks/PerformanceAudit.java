package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

/** Startup capacity sanity check; release benchmarks remain part of certification. */
public final class PerformanceAudit implements AuditCheck {
    public String id() { return "performance.capacity"; }
    public AuditStage stage() { return AuditStage.PERFORMANCE; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.ERROR; }
    public AuditResult evaluate(AuditContext context) {
        long heap = Long.parseLong(context.environment().getOrDefault("maxHeapBytes", "0"));
        int processors = Integer.parseInt(context.environment().getOrDefault("processors", "0"));
        boolean valid = heap >= 256L * 1024 * 1024 && processors >= 1;
        return result(valid ? AuditOutcome.PASS : AuditOutcome.FAIL,
                "Startup capacity processors=" + processors + " maxHeapBytes=" + heap,
                "This check is not a substitute for stress and TPS benchmarks.");
    }
}
