package yadi.samuraiai.npc.relationship;

import java.util.*;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.npc.*;

/** Owns bonds. Consumers receive defensive copies, never mutable storage. */
public final class RelationshipService {
    private static final RelationshipService INSTANCE = new RelationshipService();
    public static RelationshipService getInstance() { return INSTANCE; }
    private final Map<UUID, Map<UUID, Relationship>> bonds = new HashMap<>();
    public synchronized Optional<Relationship> find(UUID npc, UUID target) {
        if (target == null) return Optional.empty();
        return Optional.ofNullable(bonds.getOrDefault(npc, Map.of()).get(target)).map(RelationshipService::copy);
    }
    public synchronized Map<UUID, Relationship> snapshot(UUID npc) {
        Map<UUID, Relationship> result = new HashMap<>();
        bonds.getOrDefault(npc, Map.of()).forEach((id, bond) -> result.put(id, copy(bond)));
        return Map.copyOf(result);
    }
    public void adjustTrust(UUID npc, UUID target, int amount) { adjust(npc, target, amount, 0, 0, 0, 0); }
    public synchronized void adjust(UUID npc, UUID target, int trust, int respect, int hostility, int gratitude, int loyalty) {
        ServerScheduler.getInstance().requireServerThread();
        Objects.requireNonNull(npc); Objects.requireNonNull(target);
        Map<UUID, Relationship> byTarget = bonds.computeIfAbsent(npc, key -> new HashMap<>());
        boolean created = !byTarget.containsKey(target);
        Relationship bond = byTarget.computeIfAbsent(target, key -> new Relationship());
        bond.adjustTrust(trust); bond.adjustRespect(respect); bond.adjustHostility(hostility);
        bond.adjustGratitude(gratitude); bond.adjustLoyalty(loyalty);
        if (created) NPCEventBus.getInstance().post(new RelationshipCreatedEvent(npc, target));
        NPCEventBus.getInstance().post(new RelationshipUpdatedEvent(npc, target));
        NPCEventBus.getInstance().post(new RelationshipChangedEvent(npc, target));
    }
    public synchronized void remove(UUID npc, UUID target) {
        ServerScheduler.getInstance().requireServerThread();
        Map<UUID, Relationship> map = bonds.get(npc);
        if (map != null && map.remove(target) != null) {
            if (map.isEmpty()) bonds.remove(npc);
            NPCEventBus.getInstance().post(new RelationshipRemovedEvent(npc, target));
        }
    }
    public synchronized void forget(UUID npc) {
        ServerScheduler.getInstance().requireServerThread();
        for (UUID target : List.copyOf(bonds.getOrDefault(npc, Map.of()).keySet())) remove(npc, target);
        for (UUID owner : List.copyOf(bonds.keySet())) remove(owner, npc);
    }
    public synchronized void clear() { ServerScheduler.getInstance().requireServerThread(); bonds.clear(); }
    private static Relationship copy(Relationship r) {
        return new Relationship(r.getTrust(), r.getRespect(), r.getHostility(), r.getGratitude(), r.getLoyalty());
    }
}
