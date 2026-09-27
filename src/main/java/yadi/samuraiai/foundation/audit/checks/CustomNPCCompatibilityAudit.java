package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

public final class CustomNPCCompatibilityAudit implements AuditCheck {
    public String id() { return "compatibility.customnpcs"; }
    public AuditStage stage() { return AuditStage.COMPATIBILITY; }
    public String module() { return "customnpcs"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.CRITICAL; }
    public AuditResult evaluate(AuditContext context) {
        String version = context.versions().get("customnpcs");
        if (version == null) return result(AuditOutcome.NOT_APPLICABLE, "CustomNPCs absent; chat backend remains available", "No installation required.");
        return result("1.19.2.20250701".equals(version) ? AuditOutcome.PASS : AuditOutcome.FAIL,
                "Installed CustomNPCs version: " + version,
                "Version metadata only: exercise the physical adapter in GameTests before certifying compatibility.");
    }
}
