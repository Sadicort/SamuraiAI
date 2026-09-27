package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;
import java.util.List;

public final class ResourceAudit implements AuditCheck {
    public String id() { return "resources.core"; }
    public AuditStage stage() { return AuditStage.RESOURCES; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        var missing = List.of("META-INF/mods.toml", "pack.mcmeta", "samuraiai.customnpcs.mixins.json").stream()
                .filter(path -> context.resources().getResource(path) == null).toList();
        boolean managed = context.environment().getOrDefault("resources.status", "READY").equals("READY");
        return result(missing.isEmpty() && managed ? AuditOutcome.PASS : AuditOutcome.FAIL,
                missing.isEmpty() && managed ? "Startup metadata and managed directories present" :
                        "Missing=" + missing + " managedStatus=" + context.environment().getOrDefault("resources.status", "MISSING"),
                "Artifact contents and reobfuscation are also checked by verifyDistributionJar.");
    }
}
