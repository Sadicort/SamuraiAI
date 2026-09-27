package yadi.samuraiai.ai.knowledge.model;

import java.util.UUID;

/** Evidence for or against a belief: what kind, from which source and how credible. Repeated evidence from the same source counts once. */
public record ValidationEvidence(Kind kind, UUID source, double credibility, long at, String note) {
    public enum Kind { DIRECT_CONFIRMS, DIRECT_CONTRADICTS, INDEPENDENT_SOURCE, PUBLIC_EVENT, PLAYER_STATEMENT, TRUSTED_SOURCE, CONTRADICTING_SOURCE }

    public ValidationEvidence {
        credibility = Double.isFinite(credibility) ? Math.max(0.0D, Math.min(1.0D, credibility)) : 0.0D;
        note = note == null ? "" : note;
    }
}
