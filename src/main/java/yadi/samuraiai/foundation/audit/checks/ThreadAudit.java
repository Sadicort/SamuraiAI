package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

public final class ThreadAudit implements AuditCheck {
    public String id() { return "threads.async-engine"; }
    public AuditStage stage() { return AuditStage.THREADS; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        int workers = Integer.parseInt(context.environment().getOrDefault("async.workers", "0"));
        int capacity = Integer.parseInt(context.environment().getOrDefault("async.capacity", "0"));
        boolean valid = workers >= 1 && workers <= 64 && capacity >= workers && capacity <= 100000;
        return result(valid ? AuditOutcome.PASS : AuditOutcome.FAIL,
                "Global async workers=" + workers + " capacity=" + capacity,
                "Configure bounded worker and deadline pools before runtime startup.");
    }
}
