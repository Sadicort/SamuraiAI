package yadi.samuraiai.living.village.visitors;

import java.util.UUID;

/**
 * A visitor: a merchant, traveller, monk, messenger, samurai or pilgrim passing through, with or without an entity
 * ({@code npc} null = simulated only), where from, and the phase of the visit ARRIVE → STAY → INTERACT → LEAVE. Visitors eat
 * and shop while they stay (the Economy counts them as extra mouths and customers).
 */
public final class VisitorRecord {
    public enum Kind { MERCHANT, TRAVELER, MONK, MESSENGER, SAMURAI, PILGRIM }
    public enum Phase { ARRIVE, STAY, INTERACT, LEAVE, GONE }

    private final UUID id;
    private final Kind kind;
    private final String name, purpose;
    private final UUID npc, origin;
    private final long arrived;
    private long leaves;
    private Phase phase = Phase.ARRIVE;

    public VisitorRecord(UUID id, Kind kind, String name, UUID npc, UUID origin, long arrived, long leaves, String purpose) {
        this.id = id; this.kind = kind; this.name = name; this.npc = npc; this.origin = origin; this.arrived = arrived; this.leaves = Math.max(arrived + 1, leaves);
        this.purpose = purpose == null ? "" : purpose;
    }

    public UUID id() { return id; }
    public Kind kind() { return kind; }
    public String name() { return name; }
    public String purpose() { return purpose; }
    public UUID npc() { return npc; }
    public UUID origin() { return origin; }
    public long arrived() { return arrived; }
    public long leaves() { return leaves; }
    public void extend(long until) { leaves = Math.max(leaves, until); }
    public Phase phase() { return phase; }
    public void phase(Phase p) { phase = p; }
}
