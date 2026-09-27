package yadi.samuraiai.ai.relationship.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.CauseLog;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/**
 * One NPC's relationship towards one other entity. Directional: what the other thinks of this NPC is that entity's own record
 * (or nothing at all). It is a set of axes, a history, links to the memories it came from and, for every axis, the causes that
 * moved it, so the NPC can say why it distrusts someone.
 */
public final class RelationshipRecord {
    private final UUID id, source;
    private final EntityRef target;
    private RelationType type = RelationType.STRANGER;
    private RelationState state = RelationState.ACTIVE;
    private final double[] values = new double[Dimension.values().length];
    private final Map<Dimension, CauseLog> causes = new EnumMap<>(Dimension.class);
    private final List<SocialEvent> history = new ArrayList<>();
    private final Set<UUID> memories = new LinkedHashSet<>(), events = new LinkedHashSet<>();
    private FriendshipStage stage = FriendshipStage.STRANGER;
    private LoyaltyKind loyaltyKind = LoyaltyKind.PERSONAL;
    private int interactions, positive, negative, oathsKept, oathsBroken, sharedDanger, version = 1;
    private boolean loyaltyBroken;
    private long created, lastInteraction, lastDecay;

    public RelationshipRecord(UUID id, UUID source, EntityRef target, long now, double[] initial, int causeCapacity) {
        this.id = id; this.source = source; this.target = target; this.created = now; this.lastInteraction = now; this.lastDecay = now;
        System.arraycopy(initial, 0, values, 0, Math.min(initial.length, values.length));
        for (Dimension d : Dimension.values()) causes.put(d, new CauseLog(causeCapacity));
    }

    public UUID id() { return id; }
    public UUID source() { return source; }
    public EntityRef target() { return target; }
    public RelationType type() { return type; }
    public void type(RelationType v) { type = v; }
    public RelationState state() { return state; }
    public void state(RelationState v) { state = v; }
    public double get(Dimension d) { return values[d.ordinal()]; }
    public void set(Dimension d, double v) { values[d.ordinal()] = Math.max(0.0D, Math.min(100.0D, Double.isFinite(v) ? v : 0.0D)); }
    public double trust() { return get(Dimension.TRUST); }
    public double respect() { return get(Dimension.RESPECT); }
    public double affinity() { return get(Dimension.AFFINITY); }
    public double fear() { return get(Dimension.FEAR); }
    public double loyalty() { return get(Dimension.LOYALTY); }
    public double rivalry() { return get(Dimension.RIVALRY); }
    public double honor() { return get(Dimension.HONOR); }
    public CauseLog causes(Dimension d) { return causes.get(d); }
    public List<SocialEvent> history() { return history; }
    public Set<UUID> memories() { return memories; }
    public Set<UUID> events() { return events; }
    public FriendshipStage stage() { return stage; }
    public void stage(FriendshipStage v) { stage = v; }
    public LoyaltyKind loyaltyKind() { return loyaltyKind; }
    public void loyaltyKind(LoyaltyKind v) { loyaltyKind = v; }
    public int interactions() { return interactions; }
    public void interactions(int v) { interactions = Math.max(0, v); }
    public int positive() { return positive; }
    public void positive(int v) { positive = Math.max(0, v); }
    public int negative() { return negative; }
    public void negative(int v) { negative = Math.max(0, v); }
    public int oathsKept() { return oathsKept; }
    public void oathsKept(int v) { oathsKept = Math.max(0, v); }
    public int oathsBroken() { return oathsBroken; }
    public void oathsBroken(int v) { oathsBroken = Math.max(0, v); }
    public int sharedDanger() { return sharedDanger; }
    public void sharedDanger(int v) { sharedDanger = Math.max(0, v); }
    public int version() { return version; }
    public void version(int v) { version = Math.max(1, v); }
    public void bump() { version++; }
    public boolean loyaltyBroken() { return loyaltyBroken; }
    public void loyaltyBroken(boolean v) { loyaltyBroken = v; }
    public long created() { return created; }
    public long lastInteraction() { return lastInteraction; }
    public void lastInteraction(long v) { lastInteraction = v; }
    public long lastDecay() { return lastDecay; }
    public void lastDecay(long v) { lastDecay = v; }
    public void created(long v) { created = v; }
}
