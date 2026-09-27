package yadi.samuraiai.foundation.audit;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class AuditReporter {
    public void write(AuditSummary report, Path directory) throws IOException {
        Files.createDirectories(directory);
        writeAtomically(directory.resolve("FOUNDATION_STATUS.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        StringBuilder md = new StringBuilder("# Foundation startup audit\n\n");
        md.append("Run: ").append(report.runId()).append("\n\nTime: ").append(report.timestamp())
                .append("\n\nProfile: ").append(report.profile()).append("\n\nStatus: ").append(report.foundation())
                .append("\n\nStartup allowed: ").append(report.startupAllowed()).append("\n\nPhase 2 unlocked: false")
                .append("\n\nThis startup report is not a release certificate.\n\n")
                .append("| Check | Stage | Module | Outcome | Severity | Detail | Next step |\n")
                .append("|---|---|---|---|---|---|---|\n");
        for (AuditResult result : report.results()) md.append('|').append(cell(result.id())).append('|')
                .append(result.stage()).append('|').append(cell(result.module())).append('|')
                .append(result.outcome()).append('|').append(result.severity()).append('|')
                .append(cell(result.detail())).append('|').append(cell(result.suggestion())).append("|\n");
        md.append("\nAudit duration (ms): ").append(report.durationNanos() / 1_000_000d).append('\n');
        writeAtomically(directory.resolve("FOUNDATION_AUDIT.md"), md.toString());
    }
    private static String cell(String value) { return value.replace("|", "\\|").replace("\r", " ").replace("\n", " "); }
    private static void writeAtomically(Path target, String text) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), ".audit-", ".tmp");
        try {
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
