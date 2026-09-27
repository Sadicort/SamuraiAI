package yadi.samuraiai.living.economy.ledger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * The record of every economic movement (bounded: the newest {@code capacity} movements are kept in full, per settlement as
 * well as world-wide; totals by kind and resource are kept forever). "Every economic movement is recorded" means here: the
 * last movements with full provenance, and all of them as totals.
 */
public final class EconomyLedger {
    private final Deque<MovementRecord> recent = new ArrayDeque<>();
    private final Map<UUID, Deque<MovementRecord>> bySettlement = new HashMap<>();
    private final Map<MovementRecord.Kind, Map<String, Double>> totals = new EnumMap<>(MovementRecord.Kind.class);
    private int capacity = 4000, perSettlement = 400;
    private long recorded;
    private boolean dirty;

    public void configure(int max, int perSettlementMax) { capacity = Math.max(64, max); perSettlement = Math.max(16, perSettlementMax); }

    public MovementRecord record(long minute, MovementRecord.Kind kind, String resource, double quantity, UUID from, UUID to, double value, String reference, Provenance origin, UUID settlement) {
        MovementRecord r = new MovementRecord(UUID.randomUUID(), minute, kind, resource, quantity, from, to, value, reference, origin, settlement);
        add(r);
        return r;
    }

    public void add(MovementRecord r) {
        recent.addLast(r);
        while (recent.size() > capacity) recent.removeFirst();
        if (r.settlement() != null) {
            Deque<MovementRecord> q = bySettlement.computeIfAbsent(r.settlement(), k -> new ArrayDeque<>());
            q.addLast(r);
            while (q.size() > perSettlement) q.removeFirst();
        }
        totals.computeIfAbsent(r.kind(), k -> new HashMap<>()).merge(r.resource(), r.quantity(), Double::sum);
        recorded++;
        dirty = true;
    }

    public List<MovementRecord> recent(int limit) {
        List<MovementRecord> all = new ArrayList<>(recent);
        return all.size() > limit ? new ArrayList<>(all.subList(all.size() - limit, all.size())) : all;
    }

    public List<MovementRecord> of(UUID settlement, int limit) {
        List<MovementRecord> all = new ArrayList<>(bySettlement.getOrDefault(settlement, new ArrayDeque<>()));
        return all.size() > limit ? new ArrayList<>(all.subList(all.size() - limit, all.size())) : all;
    }

    public double total(MovementRecord.Kind kind, String resource) { return totals.getOrDefault(kind, Map.of()).getOrDefault(resource, 0.0D); }
    public Map<MovementRecord.Kind, Map<String, Double>> totals() { return totals; }
    public long recorded() { return recorded; }
    public void restoreRecorded(long n) { recorded = n; }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { recent.clear(); bySettlement.clear(); totals.clear(); recorded = 0; dirty = false; }
}
