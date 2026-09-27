package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

public final class CertificationAudit implements AuditCheck {
    public String id() { return "validation.certification"; }
    public AuditStage stage() { return AuditStage.VALIDATION; }
    public String module() { return "foundation"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.WARNING; }
    public AuditResult evaluate(AuditContext context) {
        return result(AuditOutcome.NOT_RUN, "Startup audit does not execute the full Foundation acceptance matrix",
                "Complete integration/regression/hardware/benchmark evidence tied to the exact artifact before READY.");
    }
}
