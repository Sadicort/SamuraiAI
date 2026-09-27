package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;

public final class DependencyAudit implements AuditCheck {
    public String id() { return "dependencies.runtime"; }
    public AuditStage stage() { return AuditStage.DEPENDENCIES; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        String forge = context.versions().getOrDefault("forge", "");
        boolean supported = "17".equals(context.environment().get("java.feature")) &&
                "1.19.2".equals(context.versions().get("minecraft")) && forge.startsWith("43.");
        return result(supported ? AuditOutcome.PASS : AuditOutcome.FAIL,
                "Java=" + context.environment().get("java.feature") + ", Minecraft=" + context.versions().get("minecraft") + ", Forge=" + forge,
                "Use Java 17, Minecraft 1.19.2 and Forge 43.x; exact release compatibility still requires tests.");
    }
}
