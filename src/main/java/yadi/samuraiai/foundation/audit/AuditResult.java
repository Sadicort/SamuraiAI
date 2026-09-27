package yadi.samuraiai.foundation.audit;

import java.util.Objects;

public record AuditResult(String id, AuditStage stage, String module, AuditSeverity severity,
                          AuditOutcome outcome, String detail, String suggestion, long durationNanos) {
    public AuditResult {
        Objects.requireNonNull(id); Objects.requireNonNull(stage); Objects.requireNonNull(module);
        Objects.requireNonNull(severity); Objects.requireNonNull(outcome);
        Objects.requireNonNull(detail); Objects.requireNonNull(suggestion);
        if (id.isBlank() || module.isBlank() || durationNanos < 0) throw new IllegalArgumentException("Invalid audit metadata");
    }
    public AuditResult timed(long nanos) {
        return new AuditResult(id, stage, module, severity, outcome, detail, suggestion, Math.max(0, nanos));
    }
    public boolean failed() { return outcome == AuditOutcome.FAIL; }
}
