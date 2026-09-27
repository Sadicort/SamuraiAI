package yadi.samuraiai.living.economy.inventory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Goods held somewhere, as lots per resource in first-in first-out order. Nothing is ever infinite: {@link #take} returns only
 * what there is, split from the oldest lots, with their origins. Perishable goods spoil by the day. The number of lots per
 * resource is bounded by merging same-origin lots of the same day and, beyond {@code maxLots}, folding the two oldest.
 */
public final class Inventory {
    private final Map<String, Deque<ResourceLot>> lots = new LinkedHashMap<>();
    private final int maxLots;
    private final int minutesPerDay;

    public Inventory(int maxLots, int minutesPerDay) { this.maxLots = Math.max(2, maxLots); this.minutesPerDay = minutesPerDay; }

    public void add(ResourceLot lot) {
        if (lot == null || lot.quantity() <= 0) return;
        Deque<ResourceLot> q = lots.computeIfAbsent(lot.resource(), k -> new ArrayDeque<>());
        ResourceLot last = q.peekLast();
        if (last != null && last.mergeable(lot, minutesPerDay)) { last.add(lot.quantity()); return; }
        q.addLast(lot);
        while (q.size() > maxLots) {
            ResourceLot a = q.pollFirst(), b = q.pollFirst();
            if (b == null) { q.addFirst(a); break; }
            // fold the two oldest lots: the older origin is kept, the quantity is summed (provenance of the rest stays in the ledger)
            q.addFirst(new ResourceLot(a.id(), a.resource(), a.quantity() + b.quantity(), (a.quality() * a.quantity() + b.quality() * b.quantity()) / Math.max(1e-9, a.quantity() + b.quantity()),
                    Math.min(a.durability(), b.durability()), a.origin(), a.originSettlement(), a.producedAt()));
        }
    }

    /** Takes up to {@code amount}, oldest first. The returned lots carry their origins. */
    public List<ResourceLot> take(String resource, double amount) {
        List<ResourceLot> out = new ArrayList<>();
        Deque<ResourceLot> q = lots.get(resource);
        double need = amount;
        while (q != null && !q.isEmpty() && need > 1e-9) {
            ResourceLot head = q.peekFirst();
            if (head.quantity() <= need + 1e-9) { out.add(q.pollFirst()); need -= head.quantity(); }
            else { out.add(head.split(need)); need = 0; }
        }
        if (q != null && q.isEmpty()) lots.remove(resource);
        return out;
    }

    public double amount(String resource) {
        Deque<ResourceLot> q = lots.get(resource);
        if (q == null) return 0;
        double t = 0;
        for (ResourceLot l : q) t += l.quantity();
        return t;
    }

    public double averageQuality(String resource) {
        Deque<ResourceLot> q = lots.get(resource);
        if (q == null) return 0;
        double t = 0, w = 0;
        for (ResourceLot l : q) { t += l.quality() * l.quantity(); w += l.quantity(); }
        return w <= 0 ? 0 : t / w;
    }

    public Map<String, Double> totals() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (var e : lots.entrySet()) { double t = 0; for (ResourceLot l : e.getValue()) t += l.quantity(); if (t > 1e-9) out.put(e.getKey(), t); }
        return out;
    }

    public double weight(ToDoubleFunction<String> unitWeight) {
        double w = 0;
        for (var e : totals().entrySet()) w += e.getValue() * unitWeight.applyAsDouble(e.getKey());
        return w;
    }

    public double value(ToDoubleFunction<String> unitValue) {
        double v = 0;
        for (var e : totals().entrySet()) v += e.getValue() * unitValue.applyAsDouble(e.getKey());
        return v;
    }

    /** Applies spoilage for {@code days}; returns how much of each resource was lost. */
    public Map<String, Double> spoil(double days, ToDoubleFunction<String> spoilPerDay) {
        Map<String, Double> lost = new LinkedHashMap<>();
        if (days <= 0) return lost;
        for (var e : lots.entrySet()) {
            double rate = spoilPerDay.applyAsDouble(e.getKey());
            if (rate <= 0) continue;
            double keep = Math.pow(1.0D - rate, days), total = 0;
            for (ResourceLot l : e.getValue()) { double loss = l.quantity() * (1.0D - keep); l.remove(loss); total += loss; }
            if (total > 1e-9) lost.put(e.getKey(), total);
            e.getValue().removeIf(l -> l.quantity() <= 1e-6);
        }
        lots.values().removeIf(Deque::isEmpty);
        return lost;
    }

    /** Destroys a share of everything (fire, looting). Returns what was lost. */
    public Map<String, Double> destroy(double fraction) {
        Map<String, Double> lost = new LinkedHashMap<>();
        double f = Math.max(0.0D, Math.min(1.0D, fraction));
        for (var e : lots.entrySet()) {
            double total = 0;
            for (ResourceLot l : e.getValue()) { double loss = l.quantity() * f; l.remove(loss); total += loss; }
            if (total > 1e-9) lost.put(e.getKey(), total);
            e.getValue().removeIf(l -> l.quantity() <= 1e-6);
        }
        lots.values().removeIf(Deque::isEmpty);
        return lost;
    }

    public List<ResourceLot> lotsOf(String resource) { Deque<ResourceLot> q = lots.get(resource); return q == null ? List.of() : List.copyOf(q); }
    public List<ResourceLot> allLots() { List<ResourceLot> out = new ArrayList<>(); for (Deque<ResourceLot> q : lots.values()) out.addAll(q); return out; }
    public boolean isEmpty() { return lots.isEmpty(); }
    public void clear() { lots.clear(); }
}
