package yadi.samuraiai.foundation.audit;

/** Discover implementations through META-INF/services, not a hardcoded list of auditors. */
public interface AuditCheck {
    String id();
    AuditStage stage();
    String module();
    AuditSeverity failureSeverity();
    AuditResult evaluate(AuditContext context) throws Exception;
    default AuditResult result(AuditOutcome outcome, String detail, String suggestion) {
        return new AuditResult(id(), stage(), module(), failureSeverity(), outcome, detail, suggestion, 0);
    }
}
