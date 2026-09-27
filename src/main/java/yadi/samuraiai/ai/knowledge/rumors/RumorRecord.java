package yadi.samuraiai.ai.knowledge.rumors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** A rumour with its whole provenance: the witness it began with, the memory behind it, every telling, every distortion and how it ended. */
public final class RumorRecord {
    private final UUID id, originMemory, traceId;
    private final EntityRef origin;
    private final String community;
    private final long created;
    private RumorClaim claim;
    private final double initialMagnitude;
    private RumorState state = RumorState.ACTIVE;
    private double strength;
    private final List<RumorHop> hops = new ArrayList<>();
    private final List<Transformation> transformations = new ArrayList<>();
    private final Set<UUID> holders = new LinkedHashSet<>();
    private long lastSpread, resolvedAt;
    private String resolvedBy = "";

    public RumorRecord(UUID id, EntityRef origin, UUID originMemory, UUID traceId, RumorClaim claim, String community, long created, double strength) {
        this.id = id; this.origin = origin; this.originMemory = originMemory; this.traceId = traceId; this.claim = claim; this.initialMagnitude = claim.magnitude();
        this.community = community == null ? "" : community; this.created = created; this.lastSpread = created; this.strength = strength;
        this.holders.add(origin.id());
    }

    public UUID id() { return id; }
    public EntityRef origin() { return origin; }
    public UUID originMemory() { return originMemory; }
    public UUID traceId() { return traceId; }
    public RumorClaim claim() { return claim; }
    public void claim(RumorClaim v) { claim = v; }
    public double initialMagnitude() { return initialMagnitude; }
    public String community() { return community; }
    public long created() { return created; }
    public RumorState state() { return state; }
    public void state(RumorState v) { state = v; }
    public double strength() { return strength; }
    public void strength(double v) { strength = Math.max(0.0D, Math.min(1.0D, v)); }
    public List<RumorHop> hops() { return hops; }
    public List<Transformation> transformations() { return transformations; }
    public Set<UUID> holders() { return holders; }
    public long lastSpread() { return lastSpread; }
    public void lastSpread(long v) { lastSpread = v; }
    public long resolvedAt() { return resolvedAt; }
    public String resolvedBy() { return resolvedBy; }
    public void resolved(long at, String by) { resolvedAt = at; resolvedBy = by == null ? "" : by; }
    public boolean open() { return state == RumorState.ACTIVE || state == RumorState.UNKNOWN; }
    public int spread() { return hops.size(); }
}
