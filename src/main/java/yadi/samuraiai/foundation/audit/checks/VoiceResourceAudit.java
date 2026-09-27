package yadi.samuraiai.foundation.audit.checks;

import com.google.gson.JsonParser;
import yadi.samuraiai.foundation.audit.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;

public final class VoiceResourceAudit implements AuditCheck {
    public String id() { return "resources.voice"; }
    public AuditStage stage() { return AuditStage.RESOURCES; }
    public String module() { return "voice"; }
    public AuditSeverity failureSeverity() { return AuditSeverity.CRITICAL; }
    public AuditResult evaluate(AuditContext context) throws Exception {
        if (context.side() == AuditContext.Side.DEDICATED_SERVER)
            return result(AuditOutcome.NOT_APPLICABLE, "Voice is client-only; no provider initialized", "None for this profile.");
        if (context.resources().getResource("io/github/ggerganov/whispercpp/WhisperCppJnaLibrary.class") == null)
            return result(AuditOutcome.FAIL, "Whisper binding not visible to game classloader", "Check minecraftLibrary and Jar-in-Jar packaging.");
        try (var stream = context.resources().getResourceAsStream("assets/samuraiai/voice/metadata/models.json")) {
            if (stream == null) return result(AuditOutcome.FAIL, "Voice manifest absent", "Restore the packaged model manifest.");
            var models = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            if (models.size() == 0) return result(AuditOutcome.FAIL, "Voice manifest empty", "Declare at least one compatible model.");
            for (var entry : models) {
                var model = entry.getAsJsonObject();
                URI uri = URI.create(model.get("url").getAsString());
                if (!model.get("id").getAsString().matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}") ||
                        model.get("size").getAsLong() <= 0 || !model.get("sha256").getAsString().matches("[a-fA-F0-9]{64}") ||
                        !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    return result(AuditOutcome.FAIL, "Invalid model metadata", "Repair ID, size, SHA256 and HTTPS origin.");
            }
            return result(AuditOutcome.PASS, "Voice binding and model metadata present; no model downloaded by audit",
                    "Native load and transcription are validated separately by Voice Core and smoke tests.");
        }
    }
}
