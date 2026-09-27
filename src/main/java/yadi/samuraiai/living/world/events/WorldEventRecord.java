package yadi.samuraiai.living.world.events;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * One world event from announcement to archive: what, where (region, optionally one settlement), how severe, why
 * ({@link #cause()}), its phase and schedule, who took part, how it was resolved and which consequences were applied.
 */
public final class WorldEventRecord {
    public enum Resolution { NONE, RESOLVED, FAILED, EXPIRED, CANCELLED }

    private final UUID id;
    private final WorldEventType type;
    private final String title;
    private final UUID region, settlement;
    private final double severity;
    private final Provenance cause;
    private final long createdAt;
    private long startAt, endAt, phaseAt, closeAt;
    private WorldEventPhase phase = WorldEventPhase.PREPARATION;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<String> tags = new LinkedHashSet<>();
    private final List<String> consequences = new ArrayList<>();
    private Resolution resolution = Resolution.NONE;
    private String resolvedBy = "", outcome = "";

    public WorldEventRecord(UUID id, WorldEventType type, String title, UUID region, UUID settlement, double severity, Provenance cause, long createdAt, long startAt, long endAt) {
        this.id = id; this.type = type; this.title = title == null ? type.name() : title; this.region = region; this.settlement = settlement;
        this.severity = Math.max(0.0D, Math.min(1.0D, severity)); this.cause = cause; this.createdAt = createdAt; this.startAt = Math.max(createdAt, startAt);
        this.endAt = Math.max(this.startAt + 1, endAt); this.phaseAt = createdAt;
    }

    public UUID id() { return id; }
    public WorldEventType type() { return type; }
    public String title() { return title; }
    public UUID region() { return region; }
    public UUID settlement() { return settlement; }
    public double severity() { return severity; }
    public Provenance cause() { return cause; }
    public long createdAt() { return createdAt; }
    public long startAt() { return startAt; }
    public long endAt() { return endAt; }
    public long closeAt() { return closeAt; }
    public long phaseAt() { return phaseAt; }
    public WorldEventPhase phase() { return phase; }
    public Set<UUID> participants() { return participants; }
    public Set<String> tags() { return tags; }
    public List<String> consequences() { return consequences; }
    public Resolution resolution() { return resolution; }
    public String resolvedBy() { return resolvedBy; }
    public String outcome() { return outcome; }
    /** Where this event belongs in the timeline. */
    public Set<String> scopes() {
        Set<String> s = new LinkedHashSet<>();
        if (region != null) s.add("region:" + region);
        if (settlement != null) s.add("settlement:" + settlement);
        s.add("world-event:" + id);
        return s;
    }

    /** When the next phase transition is due. */
    public long nextTransition() {
        return switch (phase) {
            case PREPARATION -> startAt;
            case START, DEVELOPMENT -> endAt;
            case END -> endAt;
            case CONSEQUENCES -> closeAt;
            case CLOSED -> Long.MAX_VALUE;
        };
    }

    void phase(WorldEventPhase next, long at) { phase = next; phaseAt = at; }
    void endNow(long at) { endAt = Math.max(startAt, at); }
    void startNow(long at) { startAt = at; if (endAt <= at) endAt = at + 1; }
    void closeAt(long at) { closeAt = at; }
    void resolve(Resolution r, String by, String text) { resolution = r; resolvedBy = by == null ? "" : by; outcome = text == null ? "" : text; }

    public void restore(WorldEventPhase savedPhase, long savedStart, long savedEnd, long savedPhaseAt, long savedClose, Resolution r, String by, String text) {
        phase = savedPhase; startAt = savedStart; endAt = savedEnd; phaseAt = savedPhaseAt; closeAt = savedClose; resolution = r; resolvedBy = by; outcome = text;
    }
}
