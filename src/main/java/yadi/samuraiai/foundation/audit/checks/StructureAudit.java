package yadi.samuraiai.foundation.audit.checks;

import yadi.samuraiai.foundation.audit.*;
import java.util.List;

/** Uses class resources, never Class.forName: optional/client classes are not initialized. */
public final class StructureAudit implements AuditCheck {
    public String id() { return "structure.core"; }
    public AuditStage stage() { return AuditStage.STRUCTURE; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) {
        var required = List.of("brain/Brain", "runtime/ServerScheduler", "runtime/DialogueRouter",
                "runtime/DialogueService", "behavior/Behavior", "controller/NPCController", "event/NPCEventBus",
                "memory/MemoryManager", "emotion/EmotionService", "npc/relationship/RelationshipService",
                "npc/NPCManager", "config/SamuraiSettings", "integration/IntegrationLoader",
                "foundation/audit/FoundationAuditEngine");
        var missing = required.stream().filter(name -> context.resources()
                .getResource("yadi/samuraiai/" + name + ".class") == null).toList();
        return result(missing.isEmpty() ? AuditOutcome.PASS : AuditOutcome.FAIL,
                missing.isEmpty() ? "Core responsibilities present in existing packages" : "Missing classes: " + missing,
                "Preserve existing package responsibilities; do not add empty duplicate packages.");
    }
}
