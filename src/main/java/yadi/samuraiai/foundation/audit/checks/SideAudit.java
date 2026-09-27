package yadi.samuraiai.foundation.audit.checks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import yadi.samuraiai.foundation.audit.*;

public final class SideAudit implements AuditCheck {
    private static final List<String> COMMON_BOUNDARIES = List.of(
            "yadi/samuraiai/foundation/FoundationBootstrap.class",
            "yadi/samuraiai/runtime/ServerScheduler.class",
            "yadi/samuraiai/npc/NPCManager.class");
    public String id() { return "sides.common-boundary"; }
    public AuditStage stage() { return AuditStage.SIDES; }
    public String module() { return "core"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.BLOCKER; }
    public AuditResult evaluate(AuditContext context) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String resource : COMMON_BOUNDARIES) {
            try (InputStream input = context.resources().getResourceAsStream(resource)) {
                if (input == null) { violations.add(resource + " missing"); continue; }
                String constants = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                if (constants.contains("net/minecraft/client/") || constants.contains("yadi/samuraiai/client/"))
                    violations.add(resource);
            }
        }
        return result(violations.isEmpty() ? AuditOutcome.PASS : AuditOutcome.FAIL,
                violations.isEmpty() ? "Common runtime boundaries contain no client class references" : "Client references: " + violations,
                "Move client references behind ClientBootstrap and DistExecutor.");
    }
}
