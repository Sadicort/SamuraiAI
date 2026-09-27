package yadi.samuraiai.foundation.audit.checks;

import java.util.*;
import yadi.samuraiai.foundation.audit.*;

public final class ModuleAudit implements AuditCheck {
    public String id() { return "modules.bootstrap"; }
    public AuditStage stage() { return AuditStage.MODULES; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        String status = context.environment().getOrDefault("modules.status", "MISSING");
        Set<String> ready = new HashSet<>(Arrays.asList(context.environment().getOrDefault("modules.ready", "").split(",")));
        boolean valid = status.equals("READY") && ready.containsAll(List.of("common", "brain", "dialogue", "ollama"));
        if (context.side() == AuditContext.Side.CLIENT) valid &= ready.containsAll(List.of("client", "voice"));
        return result(valid ? AuditOutcome.PASS : AuditOutcome.FAIL,
                valid ? "Module graph resolved and initialized: " + ready : "Module bootstrap status=" + status + " ready=" + ready,
                "Inspect module dependency states and rollback details in startup logs.");
    }
}
