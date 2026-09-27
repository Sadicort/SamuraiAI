package yadi.samuraiai.living.world.environment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.core.DayPhase;

/**
 * Environment interaction at full detail (LOD 0 only). It keeps the interaction points of each settlement and, from what
 * citizens are doing and the time of day, emits small orders the adapter carries out in the world: light the settlement's
 * fires at dusk and on cold or festive nights and put them out late at night; let a farmer at work tend (grow) one crop of
 * the farm; mark beds so homes are real. Doors and sleeping are already handled by navigation and the scheduler; this adds
 * what they do not cover. Orders are bounded per call.
 */
public final class EnvironmentInteractionEngine {
    public enum OrderKind { LIGHT_FIRE, DOUSE_FIRE, TEND_CROP, USE_WORKSTATION }

    public record Order(OrderKind kind, InteractionPoint point, UUID actor, String reason) { }

    private final Map<UUID, Map<String, InteractionPoint>> bySettlement = new HashMap<>();
    private final Map<UUID, Boolean> firesLit = new HashMap<>();
    private final Map<OrderKind, Long> issued = new EnumMap<>(OrderKind.class);
    private int maxOrders = 8;

    public void configure(int maxOrdersPerCall) { maxOrders = Math.max(1, maxOrdersPerCall); }

    public void register(InteractionPoint point) {
        if (point.settlement() == null) return;
        bySettlement.computeIfAbsent(point.settlement(), k -> new LinkedHashMap<>()).put(point.key(), point);
    }

    public void forgetSettlement(UUID settlement) { bySettlement.remove(settlement); firesLit.remove(settlement); }

    /** Forgets the points of one building (before it is scanned again, or when it is gone). Returns how many were dropped. */
    public int forgetBuilding(UUID settlement, UUID building) {
        Map<String, InteractionPoint> points = bySettlement.get(settlement);
        if (points == null) return 0;
        int before = points.size();
        points.values().removeIf(p -> building.equals(p.building()));
        return before - points.size();
    }

    public Collection<InteractionPoint> points(UUID settlement) { return List.copyOf(bySettlement.getOrDefault(settlement, Map.of()).values()); }

    public List<InteractionPoint> points(UUID settlement, InteractionPoint.Kind kind) {
        List<InteractionPoint> out = new ArrayList<>();
        for (InteractionPoint p : bySettlement.getOrDefault(settlement, Map.of()).values()) if (p.kind() == kind) out.add(p);
        return out;
    }

    public int count() { int n = 0; for (var m : bySettlement.values()) n += m.size(); return n; }
    public Map<OrderKind, Long> issued() { return Map.copyOf(issued); }

    /**
     * Orders for one settlement now. {@code cold} (temperature below comfort) or {@code festive} keep fires lit through the
     * night; {@code workingFarmers} are citizens currently doing farm work.
     */
    public List<Order> plan(UUID settlement, DayPhase phase, boolean cold, boolean festive, List<UUID> workingFarmers, long step) {
        List<Order> out = new ArrayList<>();
        Map<String, InteractionPoint> points = bySettlement.get(settlement);
        if (points == null || points.isEmpty()) return out;
        boolean wantLit = phase == DayPhase.SUNSET || phase == DayPhase.NIGHT || (phase == DayPhase.MIDNIGHT && (cold || festive)) || (phase == DayPhase.LATE_NIGHT && cold);
        boolean lit = firesLit.getOrDefault(settlement, false);
        if (wantLit != lit) {
            for (InteractionPoint p : points.values()) {
                if (p.kind() != InteractionPoint.Kind.FIRE || out.size() >= maxOrders) continue;
                out.add(new Order(wantLit ? OrderKind.LIGHT_FIRE : OrderKind.DOUSE_FIRE, p, null, wantLit ? (festive ? "fiesta" : cold ? "frío" : "anochece") : "noche avanzada"));
            }
            firesLit.put(settlement, wantLit);
        }
        if (!workingFarmers.isEmpty()) {
            List<InteractionPoint> crops = points(settlement, InteractionPoint.Kind.CROP);
            for (int i = 0; i < workingFarmers.size() && !crops.isEmpty() && out.size() < maxOrders; i++) {
                InteractionPoint crop = crops.get((int) Math.floorMod(step + i * 31L, (long) crops.size()));
                out.add(new Order(OrderKind.TEND_CROP, crop, workingFarmers.get(i), "riega y cuida el cultivo"));
            }
        }
        for (Order o : out) issued.merge(o.kind(), 1L, Long::sum);
        return out;
    }

    public void clear() { bySettlement.clear(); firesLit.clear(); issued.clear(); }
}
