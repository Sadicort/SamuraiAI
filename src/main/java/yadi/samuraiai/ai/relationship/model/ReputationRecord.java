package yadi.samuraiai.ai.relationship.model;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** What this NPC believes the standing of a person to be within a scope: a score per label (0-1), how sure it is and who it heard it from. */
public final class ReputationRecord {
    private final EntityRef subject;
    private final ReputationScope scope;
    private final String scopeId;
    private final Map<ReputationLabel, Double> scores = new EnumMap<>(ReputationLabel.class);
    private final Set<UUID> sources = new LinkedHashSet<>();
    private double confidence;
    private long updated;
    private int direct, hearsay;

    public ReputationRecord(EntityRef subject, ReputationScope scope, String scopeId) {
        this.subject = subject; this.scope = scope; this.scopeId = scopeId == null ? "" : scopeId;
    }

    public EntityRef subject() { return subject; }
    public ReputationScope scope() { return scope; }
    public String scopeId() { return scopeId; }
    public Map<ReputationLabel, Double> scores() { return scores; }
    public Set<UUID> sources() { return sources; }
    public double confidence() { return confidence; }
    public void confidence(double v) { confidence = Math.max(0.0D, Math.min(1.0D, v)); }
    public long updated() { return updated; }
    public void updated(long v) { updated = v; }
    public int direct() { return direct; }
    public void direct(int v) { direct = v; }
    public int hearsay() { return hearsay; }
    public void hearsay(int v) { hearsay = v; }

    public ReputationLabel dominant() {
        ReputationLabel best = ReputationLabel.UNKNOWN;
        double top = 0.05D;
        for (var e : scores.entrySet()) if (e.getKey() != ReputationLabel.UNKNOWN && e.getValue() > top) { best = e.getKey(); top = e.getValue(); }
        return best;
    }

    public double score(ReputationLabel label) { return scores.getOrDefault(label, 0.0D); }
}
