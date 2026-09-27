package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

public final class ConfigurationAudit implements AuditCheck {
    public String id() { return "configuration.snapshot"; }
    public AuditStage stage() { return AuditStage.CONFIGURATION; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        int size = Integer.parseInt(context.environment().getOrDefault("queue.size", "0"));
        int concurrent = Integer.parseInt(context.environment().getOrDefault("queue.concurrency", "0"));
        int tick = Integer.parseInt(context.environment().getOrDefault("brain.tickInterval", "0"));
        boolean valid = size >= 1 && size <= 10000 && concurrent >= 1 && concurrent <= 64 && tick >= 1 && tick <= 1200;
        return result(valid ? AuditOutcome.PASS : AuditOutcome.FAIL,
                "Validated startup snapshot: queue capacity, concurrency and brain interval",
                "Full configuration migration/reload certification remains separate.");
    }
}
