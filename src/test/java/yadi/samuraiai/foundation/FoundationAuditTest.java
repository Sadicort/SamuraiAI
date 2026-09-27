package yadi.samuraiai.foundation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.foundation.audit.*;
import yadi.samuraiai.foundation.audit.checks.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundationAuditTest {
    @TempDir Path directory;
    private AuditContext context() {
        return new AuditContext(AuditContext.Side.DEDICATED_SERVER, Map.of("minecraft", "1.19.2", "forge", "43.5.2"),
                Map.of("java.feature", "17", "queue.size", "128", "queue.concurrency", "4", "brain.tickInterval", "20",
                        "modules.status", "READY", "modules.ready", "common,brain,dialogue,ollama",
                        "async.workers", "2", "async.capacity", "256", "processors", "4", "maxHeapBytes", "1073741824"),
                getClass().getClassLoader());
    }
    private static AuditCheck check(String id, AuditStage stage, String module, AuditSeverity severity, boolean fail) {
        return new AuditCheck() {
            public String id() { return id; }
            public AuditStage stage() { return stage; }
            public String module() { return module; }
            public AuditSeverity failureSeverity() { return severity; }
            public AuditResult evaluate(AuditContext ignored) {
                if (fail) throw new IllegalStateException("fixture");
                return result(AuditOutcome.PASS, "fixture", "none");
            }
        };
    }
    @Test void registryRejectsDuplicateIds() {
        var registry = new AuditRegistry();
        var check = check("test.duplicate", AuditStage.STRUCTURE, "core", AuditSeverity.ERROR, false);
        registry.register(check);
        assertThrows(IllegalArgumentException.class, () -> registry.register(check));
    }
    @Test void registryRejectsMissingMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new AuditRegistry().register(
                check("", AuditStage.STRUCTURE, "core", AuditSeverity.ERROR, false)));
    }
    @Test void pipelineUsesMandatoryStageOrderAndExposesCoverageGaps() {
        var registry = new AuditRegistry();
        registry.register(check("last.check", AuditStage.VALIDATION, "core", AuditSeverity.ERROR, false));
        registry.register(check("first.check", AuditStage.STRUCTURE, "core", AuditSeverity.ERROR, false));
        var result = new AuditPipeline().execute(registry, context());
        assertEquals(AuditStage.STRUCTURE, result.get(0).stage());
        assertEquals(AuditStage.VALIDATION, result.get(result.size() - 1).stage());
        assertEquals(8, result.stream().filter(r -> r.outcome() == AuditOutcome.NOT_RUN).count());
    }
    @Test void brokenAuditorDoesNotSuppressRemainingChecks() {
        var registry = new AuditRegistry();
        registry.register(check("first.failure", AuditStage.STRUCTURE, "core", AuditSeverity.ERROR, true));
        registry.register(check("second.pass", AuditStage.DEPENDENCIES, "core", AuditSeverity.ERROR, false));
        var result = new AuditPipeline().execute(registry, context());
        assertTrue(result.stream().anyMatch(r -> r.id().equals("first.failure") && r.failed()));
        assertTrue(result.stream().anyMatch(r -> r.id().equals("second.pass") && r.outcome() == AuditOutcome.PASS));
    }
    @Test void blockerStopsCoreWhileCriticalOnlyDisablesAffectedModule() {
        var critical = new AuditResult("voice.failure", AuditStage.RESOURCES, "voice", AuditSeverity.CRITICAL,
                AuditOutcome.FAIL, "bad model manifest", "repair", 1);
        var summary = AuditSummary.startup(context(), List.of(critical), 1);
        assertTrue(summary.startupAllowed());
        assertEquals(List.of("voice"), summary.disabledModules());
        assertFalse(summary.phase2Unlocked());
        var blocker = new AuditResult("core.failure", AuditStage.DEPENDENCIES, "core", AuditSeverity.BLOCKER,
                AuditOutcome.FAIL, "missing core", "repair", 1);
        assertFalse(AuditSummary.startup(context(), List.of(blocker), 1).startupAllowed());
    }
    @Test void passingStartupIsNeverMisrepresentedAsCertification() throws Exception {
        var passing = check("test.pass", AuditStage.STRUCTURE, "core", AuditSeverity.BLOCKER, false).evaluate(context());
        var summary = AuditSummary.startup(context(), List.of(passing), 1);
        assertEquals("PARTIAL", summary.foundation());
        assertTrue(summary.startupAllowed());
        assertFalse(summary.phase2Unlocked());
    }
    @Test void emptyDiscoveryCannotPassSilently() {
        var results = new AuditPipeline().execute(new AuditRegistry(), context());
        assertEquals("BLOCKED", AuditSummary.startup(context(), results, 1).foundation());
    }
    @Test void serviceLoaderDiscoversRealChecks() {
        var registry = AuditRegistry.discover(getClass().getClassLoader());
        assertTrue(registry.snapshot().stream().anyMatch(c -> c.id().equals("structure.core")));
        assertTrue(registry.snapshot().stream().anyMatch(c -> c.id().equals("resources.voice")));
        var report = new FoundationAuditEngine().run(context());
        assertTrue(report.startupAllowed(), report.results().toString());
        assertEquals("PARTIAL", report.foundation());
    }
    @Test void serverVoiceCheckDoesNotInspectOrLoadClientProvider() throws Exception {
        ClassLoader unavailable = new ClassLoader(null) {
            @Override public java.net.URL getResource(String name) { throw new AssertionError("Server touched voice resource"); }
        };
        var context = new AuditContext(AuditContext.Side.DEDICATED_SERVER, Map.of(), Map.of(), unavailable);
        assertEquals(AuditOutcome.NOT_APPLICABLE, new VoiceResourceAudit().evaluate(context).outcome());
    }
    @Test void unsupportedOptionalVersionDisablesAdapterWithoutBlockingCore() {
        var original = context();
        var context = new AuditContext(original.side(), Map.of("customnpcs", "unknown"), original.environment(), original.resources());
        var result = new CustomNPCCompatibilityAudit().evaluate(context);
        var report = AuditSummary.startup(context, List.of(result), 1);
        assertTrue(report.startupAllowed());
        assertEquals(List.of("customnpcs"), report.disabledModules());
    }
    @Test void reporterWritesParseableJsonAndMatchingMarkdownRunId() throws Exception {
        var report = new FoundationAuditEngine().run(context());
        new AuditReporter().write(report, directory);
        var json = JsonParser.parseString(Files.readString(directory.resolve("FOUNDATION_STATUS.json"))).getAsJsonObject();
        assertEquals(report.runId(), json.get("runId").getAsString());
        assertFalse(json.get("phase2Unlocked").getAsBoolean());
        assertTrue(Files.readString(directory.resolve("FOUNDATION_AUDIT.md")).contains(report.runId()));
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }
}
